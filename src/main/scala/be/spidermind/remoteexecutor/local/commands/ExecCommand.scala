package be.spidermind.remoteexecutor.local.commands

import akka.util.Timeout
import be.spidermind.remoteexecutor.RemoteMessages

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.pattern.ask
import be.spidermind.remoteexecutor.local.commands.types.{CommandHelper, CommandLineHandler}

/**
  * Created by anonyme77 on 16/08/2017.
  */
class ExecCommand extends CommandLineHandler {
    override def cmdKey(): String = "exec"

    override def execCmd(line: String): Unit = {
        implicit val timeout:Timeout = 10
        val fResult = localActor?RemoteMessages.ExecCommand(line)
        Await.ready(fResult,Duration.Inf)
    }

    override def help(): CommandHelper =
        new CommandHelper("exec",
            "exec a shell command on all connected computers",
            "exec cmd"
        )
}
