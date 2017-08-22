package be.spidermind.remoteexecutor.local.commands

import java.nio.file.{Files, Paths}

import be.spidermind.remoteexecutor.RemoteMessages
import be.spidermind.remoteexecutor.local.commands.types.{CommandHelper, CommandLineHandler}
import be.spidermind.remoteexecutor.local.interpreter.Interpreter

/**
  * Created by anonyme77 on 22/08/2017.
  */
class ReadConfigCommand extends CommandLineHandler {
    override def execCmd(file: String): Unit = {
        if(Files.exists(Paths.get(file))) {
            scala.io.Source.fromFile(file).getLines().foreach {
                l =>
                    val definition = l.split("=")
                    val name = definition(0)
                    val ip = definition(1).split(":")(0)
                    val port = definition(1).split(":")(1)
                    println(name)
                    println(ip)
                    println(port)
                    localActor!RemoteMessages.Connect(name, ip, port)
            }
        } else {
            throwError("Impossible to find config file ["+file+"]")
        }
    }

    override def cmdKey(): String = "load"

    override def help(): CommandHelper = new CommandHelper("load",
        "load remote host definition from file",
        "load remote.cfg")
}
