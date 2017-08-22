package be.spidermind.remoteexecutor.local

import java.io.File

import akka.actor.{ActorRef, ActorSystem, Props}
import be.spidermind.remoteexecutor.common.FreePortFinder
import be.spidermind.remoteexecutor.local.commands._
import be.spidermind.remoteexecutor.local.commands.types.CommandLineHandler
import be.spidermind.remoteexecutor.local.interpreter.Interpreter
import com.typesafe.config.ConfigFactory

import scala.io.StdIn
import scala.util.{Failure, Try}

/**
  * Created by anonyme77 on 18/08/2017.
  */
object CommandLine {
    private var localActor:ActorRef = null
    private var system:ActorSystem = null
    private val default_remote_config = "remote.cfg"

    def main(args: Array[String]) {
        initialize()

        connectConfiguredRemotes()

        var ln = StdIn.readLine("\n\rremote> ")
        while(ln != "exit") {
            Interpreter.execCommandLine(ln)
            ln = StdIn.readLine("\n\rremote> ")
        }

        system.shutdown()
    }

    private def initialize() = {

        FreePortFinder.beginPortsDiscovery()

        val configFile = getClass.getClassLoader.getResource("local_application.conf").getFile
        val config = ConfigFactory.parseFile(new File(configFile))
        system = ActorSystem("ClientSystem",config)
        localActor = system.actorOf(Props[LocalActor], name="local")

        var cmd:CommandLineHandler = new BalanceCommand()
        cmd.setRemotes(localActor)
        Interpreter.addCommand(cmd.cmdKey(),cmd)

        cmd = new ConnectCommand()
        cmd.setRemotes(localActor)
        Interpreter.addCommand(cmd.cmdKey(),cmd)

        cmd = new ConnectedCommand()
        cmd.setRemotes(localActor)
        Interpreter.addCommand(cmd.cmdKey(),cmd)

        cmd = new DisconnectCommand()
        cmd.setRemotes(localActor)
        Interpreter.addCommand(cmd.cmdKey(),cmd)

        cmd = new ExecCommand()
        cmd.setRemotes(localActor)
        Interpreter.addCommand(cmd.cmdKey(),cmd)

        cmd = new HelpCommand()
        cmd.setRemotes(localActor)
        Interpreter.addCommand(cmd.cmdKey(),cmd)

        cmd = new PythonCommand()
        cmd.setRemotes(localActor)
        Interpreter.addCommand(cmd.cmdKey(),cmd)

        cmd = new ScriptCommand()
        cmd.setRemotes(localActor)
        Interpreter.addCommand(cmd.cmdKey(),cmd)

        cmd = new UploadCommand()
        cmd.setRemotes(localActor)
        Interpreter.addCommand(cmd.cmdKey(),cmd)

        cmd = new ReadConfigCommand()
        cmd.setRemotes(localActor)
        Interpreter.addCommand(cmd.cmdKey(),cmd)

        val directedCmd = new DownloadCommand()
        directedCmd.setRemotes(localActor)
        Interpreter.addDirectedCommand(cmd.cmdKey(), directedCmd)
    }

    private def connectConfiguredRemotes() = {
        Interpreter.cmdLineMap()("load").execute(default_remote_config)
    }
}
