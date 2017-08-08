package com.madhukaraphatak.akka.local

import java.io.{File, InputStream, PrintWriter}
import java.nio.file.{Files, Paths}

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.madhukaraphatak.akka.{RemoteMessages, WriterActor}
import com.madhukaraphatak.akka.RemoteMessages.LoadBalance
import com.madhukaraphatak.akka.local.cmd.getClass
import com.typesafe.config.ConfigFactory

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.io.{Codec, Source, StdIn}
import scala.sys.process.{Process, ProcessIO}
import scala.util.{Failure, Success, Try}

/**
  * Local actor which listens on any free port
  */
object LocalActor {
    val MAX_MSG_SIZE = 125000
}

class LocalActor extends Actor{
    import context.dispatcher

    private val remoteActors:collection.mutable.HashMap[String, ActorSelection] =
        collection.mutable.HashMap[String, ActorSelection]()

    private val downloads:collection.mutable.HashMap[String, (String, collection.mutable.ListBuffer[Byte])] =
        collection.mutable.HashMap[String, (String, collection.mutable.ListBuffer[Byte])]()

    private val writer = context.actorOf(Props[WriterActor])

    private val toReveiveBalance = collection.mutable.HashMap[String, Integer]()
    private val localFileBalance = collection.mutable.HashMap[String, String]()

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
            val process = Process ("python "+scriptPath)
            val io = new ProcessIO (
                writer,
                out => {scala.io.Source.fromInputStream(out).getLines.foreach{
                    l => remoteActors.keys.foreach {
                        k =>
                            writer!RemoteMessages.ExecutionResult("Send output ["+l+"] to remote ["+k+"]")
                            remoteActors(k) ! RemoteMessages.AddInputToProcess(execName,l)
                            if(!toReveiveBalance.contains(execName)) toReveiveBalance.put(execName,0)
                            toReveiveBalance.put(execName, toReveiveBalance(execName) + 1)
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
            toReveiveBalance.put(name, toReveiveBalance(name) - 1)
            if(toReveiveBalance(name) == 0) {
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

            val content = lines.replace("# file",script).replace("print(\"main\")",prodFnct+"()")
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



object cmd {

    private var localActor:ActorRef = null

    private val functionMap:Map[String, Tuple2[(String) => Any, String]]
            = Map[String, Tuple2[(String) => Any, String]](
        "exec" -> Tuple2(exec, "-(exemple)-> exec cmd -(explic.)-> exec a shell command on all connected computers"),
        "upload" -> Tuple2(upload, "-(exemple)-> upload from to -(explic.)-> upload 'from' local file to 'to' file path on all connected computers\""),
        "connect" -> Tuple2(connect, "-(exemple)-> connect name 127.0.0.1 5150 -(explic.)-> connect computer on address 127.0.0.1 on port 5150 with name 'name'\""),
        "connected" -> Tuple2(connected, "-(exemple)-> connected ls -(explic.)-> show all connected computers\""),
        "disconnect" -> Tuple2(disonnect, "-(exemple)-> disconnect name -(explic.)-> disconnect computer identified by 'name'\""),
        "python" -> Tuple2(python, "-(exemple)-> python from to -(explic.)-> upload 'from' python script to all computers on 'to' file path and execute the script with python on all connected computers\""),
        "help" -> Tuple2(help, "-(exemple)-> help ls -(explic)-> print this helper"),
        "balance" -> Tuple2(balance, "-(exemple)-> balance name producer:/path/to/script:function1 consumer:/path/to/script:function2 -(explic.)-> create a system with name 'name' where we execute function1 from producer script and give its outputs to function2 on consumer script")
    )

    private val directFunctionMap = Map[String, ((String,String) => Any, String)](
        "download" -> Tuple2(download, "-(exemple)-> #name download from to -(explic.)-> download the file 'from' from 'name' computer to local 'to' file path\"")
    )

    def main(args: Array[String]) {
        val configFile = getClass.getClassLoader.getResource("local_application.conf").getFile
        val config = ConfigFactory.parseFile(new File(configFile))
        val system = ActorSystem("ClientSystem",config)
        localActor = system.actorOf(Props[LocalActor], name="local")

        var ln = StdIn.readLine("\n\rremote> ")
        while(ln != "exit") {
            Try{
                val functionName = getFunction(ln)
                if(functionName startsWith("#")) {
                    val remote = functionName.replace("#","")
                    val function = getFunction(getArgs(ln))
                    val args = getArgs(getArgs(ln))

                    if(!directFunctionMap.contains(function)) {
                        println("Fonction inconnue")
                    } else {
                        directFunctionMap(function)._1(remote,args)
                    }

                } else if(functionMap.contains(functionName)) {
                    functionMap(functionName)._1(getArgs(ln))
                } else {
                    exec(ln)
                }
            } match {
                case Failure(ex) => println("Une erreur est survenue : "+ex.getMessage)
                case _ =>
            }

            ln = StdIn.readLine("\n\rremote> ")
        }
        system.shutdown()
    }

    private def balance(args:String) = {
        val majorParts = args.split(" ")
        val name = majorParts(0)
        val producerPart = majorParts(1).split(":")
        val producerScript = producerPart(1)
        val producerFunc = producerPart(2)
        val consumerPart = majorParts(2).split(":")
        val consumerScript = consumerPart(1)
        val consumerFunc = consumerPart(2)

        upload(consumerScript+" "+consumerScript.split("/").last)

        localActor!LoadBalance(name, producerScript, producerFunc,
            consumerScript, consumerFunc)
    }

    private def help(args:String) = {
        println("###########################################")
        println("--------> Commandes")
        println("###########################################")
        functionMap.keys.foreach { k =>
            println("Commande : "+k+" "+functionMap(k)._2)
        }

        println("\n###########################################")
        println("--------> Commandes dirigées")
        println("###########################################")
        directFunctionMap.keys.foreach { k =>
            println("Commande : "+k+" "+directFunctionMap(k)._2)
        }
    }

    private def download(remote:String, args:String): Unit = {
        val from = args.split(" ")(0)
        val to = args.split(" ")(1)

        localActor!RemoteMessages.Download(remote, from, to)
    }

    private def python(args:String):Unit = {
        val to = args.split(" ")(1)
        upload(args)
        exec("python "+to)
        exec("rm "+to)
    }

    private def disonnect(args:String): Unit = {
        args split(" ") foreach {
            name => localActor ! RemoteMessages.Disconnect(name)
        }
    }

    private def connected(args:String) = {
        localActor!RemoteMessages.Connected()
    }

    private def connect(argsString:String) = {
        val args = argsString split(" ")
        val name = args(0)
        val ip = args(1)
        val port = args(2)

        localActor!RemoteMessages.Connect(name, ip, port)
    }

    private def exec(cmd:String) = {
        implicit val timeout:Timeout = 10
        val fResult = localActor?RemoteMessages.ExecCommand(cmd)
        Await.ready(fResult,Duration.Inf)
    }

    private def upload(args:String) = {
        val from = args.split(" ")(0)
        val to = args.split(" ")(1)
        val byteArray = Files.readAllBytes(Paths.get(from))
        localActor!RemoteMessages.UploadFile(byteArray,to)
    }

    private def getFunction(ln:String) = {
        ln.split(" ")(0)
    }

    private def getArgs(ln:String) = {
        val words = ln split(" ")
        words.slice(1, words.length).mkString(" ")
    }

}
