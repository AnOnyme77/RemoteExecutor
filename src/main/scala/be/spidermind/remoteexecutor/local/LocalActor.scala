package be.spidermind.remoteexecutor.local

import java.io.{File, InputStream, PrintWriter}
import java.nio.file.{Files, Paths}
import java.util.Random
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import be.spidermind.remoteexecutor.{RemoteMessages, WriterActor}
import be.spidermind.remoteexecutor.RemoteMessages.LoadBalance
import be.spidermind.remoteexecutor.local.commands._
import be.spidermind.remoteexecutor.local.commands.types.CommandLineHandler
import be.spidermind.remoteexecutor.local.interpreter.Interpreter
import com.typesafe.config.ConfigFactory
import play.api.libs.json.{JsArray, Json}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.io.{Codec, Source, StdIn}
import scala.sys.process.{Process, ProcessIO}
import scala.util.{Failure, Success, Try}
import scalaj.http.Http

/**
  * Local actor which listens on any free port
  */
object LocalActor {
    val MAX_MSG_SIZE = 125000
}

class LocalActor extends Actor {
    import context.dispatcher

    private val remoteActors:collection.mutable.HashMap[String, ActorSelection] =
        collection.mutable.HashMap[String, ActorSelection]()

    private val downloads:collection.mutable.HashMap[String, (String, collection.mutable.ListBuffer[Byte])] =
        collection.mutable.HashMap[String, (String, collection.mutable.ListBuffer[Byte])]()

    private val writer = context.actorOf(Props[WriterActor])

    private val toReceiveBalance = collection.mutable.HashMap[String, Integer]()
    private val localFileBalance = collection.mutable.HashMap[String, String]()
    private val localBalanceFinished = collection.mutable.HashMap[String, Boolean]()

    def readFile(path:String) = {
        val source = scala.io.Source.fromFile(path)
        val lines = try source.mkString finally source.close()
        lines
    }

    def readLocalBalanceFile() = {
        val stream : InputStream = getClass.getResourceAsStream("/balance_local.py")
        val lines = scala.io.Source.fromInputStream( stream ).mkString("")
        lines
    }

    def writeToFile(path:String, content:String) = {
        val writer = new PrintWriter(new File(path))
        writer.write(content)
        writer.close()
    }

    def execProducer(scriptPath:String, execName:String) = {
        Future{
            localFileBalance.put(execName, scriptPath)
            localBalanceFinished.put(execName, false)
            val process = Process ("python "+scriptPath)
            val io = new ProcessIO (
                writer,
                out => {
                    context.system.scheduler.schedule(Duration(7, TimeUnit.SECONDS), Duration(1, TimeUnit.SECONDS)) {
                        val response = Http("http://127.0.0.1:9000/")
                            .timeout(connTimeoutMs = 5000, readTimeoutMs = 5000)
                            .asString
                            .body
                        val jsonBody = Json.parse(response)
                        val values = jsonBody.as[JsArray]
                        values.value.foreach {
                            jsValue =>
                                val value = jsValue.as[String]
                                if(!value.equals("#!#/#%END%#\\#!#")) {
                                    val rand = new Random(System.currentTimeMillis())
                                    val random_index = rand.nextInt(remoteActors.size)
                                    val remoteName = remoteActors.keys.toArray.apply(random_index)

                                    writer ! RemoteMessages.ExecutionResult("Send output [" + value + "] to remote [" + remoteName + "]")
                                    remoteActors(remoteName) ! RemoteMessages.AddInputToProcess(execName, value)
                                    if (!toReceiveBalance.contains(execName)) toReceiveBalance.put(execName, 0)
                                    toReceiveBalance.put(execName, toReceiveBalance(execName) + 1)
                                } else {
                                    writer ! RemoteMessages.ExecutionResult("Local balance ["+execName+"] finished")
                                    localBalanceFinished.put(execName, true)
                                }

                        }
                    }},
                err => {scala.io.Source.fromInputStream(err).getLines.foreach(println)})
            process run io
        }
    }

    def writer(output: java.io.OutputStream) = {

    }

    override def receive: Receive = {

        case RemoteMessages.RemoteProcessResult(name, output) =>
            writer!RemoteMessages.RemoteProcessResult(name, output)
            toReceiveBalance.put(name, toReceiveBalance(name) - 1)
            if(toReceiveBalance(name) == 0 && localBalanceFinished(name)) {
                writer!RemoteMessages.ExecutionResult("Execution of balance ["+name+"] terminated")
                remoteActors.values.foreach {
                    r => r ! RemoteMessages.TerminateProcess(name)
                }
                new File(localFileBalance(name)).delete()
            }

        case RemoteMessages.LoadBalance(name, prodSc, prodFnct, consSc, consFnct) =>
            remoteActors.keys.foreach {
                a => remoteActors(a)!RemoteMessages.SpawnProcess(name, consSc.split("/").last, consFnct)
            }

            val script = readFile(prodSc)

            val lines = readLocalBalanceFile()

            val content = lines.replace("# file",script).replace("print(\"main\")",prodFnct+"(self.queue)")
            val fileName = name+".py"
            writeToFile(fileName,content)

            execProducer(fileName, name)

        case RemoteMessages.DownloadEnd(path) =>
            Files.write(Paths.get(downloads(path)._1),downloads(path)._2.toArray)
            downloads.remove(path)

        case RemoteMessages.DownloadData(data, path)=>
            val pair = downloads(path)
            var actualData = pair._2
            data.foreach{
                b => actualData = actualData.+=(b)
            }
            downloads.put(path, (pair._1, actualData))

        case RemoteMessages.Download(remote, from, to) =>
                downloads.put(from, (to, collection.mutable.ListBuffer()))
                remoteActors(remote)!RemoteMessages.DownloadStart(from)

        case RemoteMessages.Disconnect(name) =>
            remoteActors.remove(name)
        case RemoteMessages.Connect(name, ip, port) =>
            val actorSelection = context.actorSelection("akka.tcp://RemoteSystem@"+ip+":"+port+"/user/remote")
            remoteActors.put(name, actorSelection)
            writer!RemoteMessages.ExecutionResult("Connexion réussie")
        case RemoteMessages.Connected() =>
            remoteActors.keys.foreach{
                s => writer!RemoteMessages.ExecutionResult("Connecté à "+s)
            }
        case RemoteMessages.ExecCommands(cmds) =>
            remoteActors.values.foreach {
                remoteActor => remoteActor ! RemoteMessages.ExecCommands(cmds)
            }
        case RemoteMessages.ExecCommand(cmd) =>
            remoteActors.values.foreach {
                remoteActor => remoteActor ! RemoteMessages.ExecCommand(cmd)
            }
        case RemoteMessages.UploadFile(array, file) =>
            val nbMess = array.length / LocalActor.MAX_MSG_SIZE
            if(nbMess==0) {
                remoteActors.values.foreach {
                    remoteActor =>
                        remoteActor ! RemoteMessages.UploadFile(array, file)
                        remoteActor ! RemoteMessages.UploadEnd(file)
                }
            }
            else {
                remoteActors.values.foreach {
                    remoteActor => remoteActor ! RemoteMessages.UploadFile(array.slice(0, LocalActor.MAX_MSG_SIZE), file)
                }
                self!RemoteMessages.UploadFile(array.slice(LocalActor.MAX_MSG_SIZE, array.length), file)
            }

        case msg:String => {
            writer!RemoteMessages.ExecutionResult(msg)
        }
        case RemoteMessages.ExecutionResult(output) =>
            writer!RemoteMessages.ExecutionResult(output)

    }
}
