package com.madhukaraphatak.akka.remote

import java.io.File
import java.nio.charset.Charset
import java.nio.file.{Files, Paths}

import akka.actor._
import com.madhukaraphatak.akka.RemoteMessages
import com.typesafe.config.ConfigFactory

import scala.sys.process._

/**
  * Remote actor which listens on port 5150
  */
class RemoteActor extends Actor {

    private val uploadingFiles = collection.mutable.HashMap[String, collection.mutable.ListBuffer[Byte]]()

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
        case _ => println("Received unknown msg ")
    }
}

object RemoteActor{
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


