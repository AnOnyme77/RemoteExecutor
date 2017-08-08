package com.madhukaraphatak.akka.remote

import java.io.{File, InputStream, OutputStream, PrintWriter}
import java.nio.charset.Charset
import java.nio.file.{Files, Paths}
import java.util.concurrent.TimeUnit

import akka.actor._
import com.madhukaraphatak.akka.RemoteMessages
import com.typesafe.config.ConfigFactory
import play.api.libs.json.{JsArray, Json}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.sys.process._
import scala.util.{Failure, Try}
import scalaj.http.{Http, HttpRequest}


/**
  * Remote actor which listens on port 5150
  */
class RemoteActor extends Actor {
    import context.dispatcher

    private val uploadingFiles = collection.mutable.HashMap[String, collection.mutable.ListBuffer[Byte]]()
    private val runningProcess = collection.mutable.HashMap[String, Process]()
    private val runningProcessWaiting = collection.mutable.HashMap[String, collection.mutable.ListBuffer[String]]()
    private val runningProcessReady = collection.mutable.HashMap[String, Boolean]()
    private val runningProcessClient = collection.mutable.HashMap[String, ActorRef]()
    private val runningProcessFile = collection.mutable.HashMap[String, Tuple2[String, String]]()

    def sendResult(name:String): (String) => Unit = {
        {
            o => runningProcessClient(name)!RemoteMessages.RemoteProcessResult(name, o)
        }
    }

    def sendLateInputs(name:String) = {
        if(runningProcessWaiting.contains(name) && runningProcessReady(name)) {
            runningProcessWaiting(name).foreach {
                l =>
                    Http("http://127.0.0.1:8080/").postForm(Seq("line" -> l)).asString
            }
        }
    }

    def getMessages(name:String) = {
        val response = Http("http://127.0.0.1:8080/").asString.body
        val jsonBody = Json.parse(response)
        val values = jsonBody.as[JsArray]
        values.value.foreach {
            jsValue =>
                val value = jsValue.as[String]
                runningProcessClient(name) ! RemoteMessages.RemoteProcessResult(name, jsValue)
        }

    }

    def execConsumer(name:String, scriptPath:String) = {
        val process = Process ("python "+scriptPath)
        val io = new ProcessIO (
            {stream =>
                Thread.sleep(7000)
                runningProcessReady.put(name,true)
                context.system.scheduler.schedule(Duration(1, TimeUnit.SECONDS), Duration(1, TimeUnit.SECONDS)) {
                    getMessages(name)
                }
                sendLateInputs(name)},
            out => {},
            err => {scala.io.Source.fromInputStream(err).getLines.foreach(println)})

        runningProcess.put(name, process run io)
    }

    def readFile(path:String) = {
        val source = scala.io.Source.fromFile(path)
        val lines = try source.mkString finally source.close()
        lines
    }

    def readRemoteBalanceFile() = {
        val stream : InputStream = getClass.getResourceAsStream("/balance_remote.py")
        val lines = scala.io.Source.fromInputStream( stream ).mkString("")
        lines
    }

    def writeToFile(path:String, content:String) = {
        val writer = new PrintWriter(new File(path))
        writer.write(content)
        writer.close()
    }

    def waitUploadEnd: Receive = {
        case RemoteMessages.UploadFile(file, dest) =>
            file.foreach{
                b => uploadingFiles(dest) = uploadingFiles(dest).+=(b)
            }
        case RemoteMessages.UploadEnd(file) =>
            Files.write(Paths.get(file),uploadingFiles(file).toArray)
            uploadingFiles.remove(file)
            context.become(receive)
    }

    override def receive: Receive = {
        case RemoteMessages.DownloadStart(path) =>
            val byteArray = Files.readAllBytes(Paths.get(path))
            var remainingSize = byteArray.length
            var sended = 0
            while(remainingSize>0) {
                if(remainingSize > RemoteActor.MAX_MSG_SIZE) {
                    sender!RemoteMessages.DownloadData(byteArray.slice(sended,sended + RemoteActor.MAX_MSG_SIZE),path)
                    remainingSize-=RemoteActor.MAX_MSG_SIZE
                    sended+=RemoteActor.MAX_MSG_SIZE
                } else {
                    sender!RemoteMessages.DownloadData(byteArray.slice(sended,sended + remainingSize),path)
                    sender!RemoteMessages.DownloadEnd(path)
                    remainingSize = 0
                    sended = remainingSize
                }
            }

        case msg: String => {
            println("remote received " + msg + " from " + sender)
            sender ! "hi"
        }
        case RemoteMessages.ExecCommand(f) => sender!RemoteMessages.ExecutionResult(f.!!)
        case RemoteMessages.ExecCommands(commands) => {
            val results = commands map { c => c.!! }
            sender ! RemoteMessages.ExecutionResult(results)
        }
        case RemoteMessages.UploadFile(file, dest) =>
            uploadingFiles.put(dest, collection.mutable.ListBuffer())
            file.foreach{
                b => uploadingFiles(dest) = uploadingFiles(dest).+=(b)
            }
            context.become(waitUploadEnd)
        case RemoteMessages.SpawnProcess(name, fileName, function) =>
            val script = readFile(fileName)
            val completeScript = readRemoteBalanceFile().replace("# fctCode",script).replace("# fctCall",function+"(elem)")
            val scriptName = fileName+"_remote.py"
            writeToFile(scriptName,completeScript)
            runningProcessClient.put(name, sender)
            runningProcessFile.put(name, (scriptName, fileName))
            execConsumer(name, scriptName)

        case RemoteMessages.AddInputToProcess(name, line) =>
            if(runningProcessReady.contains(name) && runningProcessReady(name)) {
                sendLateInputs(name)
                Http("http://127.0.0.1:8080/").postForm(Seq("line" -> line)).asString
            } else {
                if(!runningProcessWaiting.contains(name)) runningProcessWaiting.put(name, collection.mutable.ListBuffer())
                runningProcessWaiting(name).append(line)
            }

        case RemoteMessages.TerminateProcess(name) =>
            runningProcess(name).destroy()
            runningProcess.remove(name)
            runningProcessClient.remove(name)
            runningProcessReady.remove(name)
            runningProcessWaiting.remove(name)
            var path = runningProcessFile(name)._1
            new File(path).delete()
            path = runningProcessFile(name)._2
            new File(path).delete()
            runningProcessFile.remove(name)

        case _ => println("Received unknown msg ")
    }
}

object RemoteActor{

    val MAX_MSG_SIZE = 125000

    def main(args: Array[String]) {
        //get the configuration file from classpath
        val configFile = getClass.getClassLoader.getResource("remote_application.conf").getFile
        //parse the config
        val config = ConfigFactory.parseFile(new File(configFile))
        //create an actor system with that config
        val system = ActorSystem("RemoteSystem" , config)
        //create a remote actor from actorSystem
        val remote = system.actorOf(Props[RemoteActor], name="remote")
        println("remote is ready")


    }
}


