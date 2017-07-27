package com.madhukaraphatak.akka.local

import java.io.File
import java.nio.file.{Files, Paths}

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.madhukaraphatak.akka.RemoteMessages
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn
import scala.util.{Failure, Success, Try}

/**
  * Local actor which listens on any free port
  */
object LocalActor {
    val MAX_MSG_SIZE = 125000
}

class LocalActor extends Actor{

    private val remoteActors:collection.mutable.HashMap[String, ActorSelection] =
        collection.mutable.HashMap[String, ActorSelection]()

    override def receive: Receive = {
        case RemoteMessages.Disconnect(name) =>
            remoteActors.remove(name)
        case RemoteMessages.Connect(name, ip, port) =>
            val actorSelection = context.actorSelection("akka.tcp://RemoteSystem@"+ip+":"+port+"/user/remote")
            remoteActors.put(name, actorSelection)
        case RemoteMessages.Connected() =>
            remoteActors.keys.foreach{
                s => println("Connected to "+s)
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
            println("got message from remote" + msg)
        }
        case RemoteMessages.ExecutionResult(output) => println(output)
    }
}



object cmd {

    private var localActor:ActorRef = null

    private val functionMap = Map[String, (String) => Any](
        "exec" -> exec,
        "upload" -> upload,
        "connect" -> connect,
        "connected" -> connected,
        "disconnect" -> disonnect,
        "python" -> python
    )

    def main(args: Array[String]) {
        val configFile = getClass.getClassLoader.getResource("local_application.conf").getFile
        val config = ConfigFactory.parseFile(new File(configFile))
        val system = ActorSystem("ClientSystem",config)
        localActor = system.actorOf(Props[LocalActor], name="local")

        var ln = StdIn.readLine("remote> ")
        while(ln != "exit") {
            Try{
                val functionName = getFunction(ln)
                if(functionMap.contains(functionName)) {
                    functionMap(functionName)(getArgs(ln))
                } else {
                    exec(ln)
                }
            } match {
                case Failure(ex) => println("Une erreur est survenue : "+ex.getMessage)
                case _ =>
            }

            ln = StdIn.readLine("remote> ")
        }
        system.shutdown()
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
        println(from)
        println(to)
        println(byteArray.length)
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
