package be.spidermind.remoteexecutor.local.commands

import be.spidermind.remoteexecutor.RemoteMessages
import be.spidermind.remoteexecutor.annotations.{CommandLine, CommandLineHelp}
import be.spidermind.remoteexecutor.local.commands.types.CommandLineHandler

/**
  * Created by anonyme77 on 16/08/2017.
  */
@CommandLine
@CommandLineHelp(cmd = "disconnect",
    example="disconnect name",
    explanation = "disconnect computer identified by 'name'")
class DisconnectCommand extends CommandLineHandler {
    override def cmdKey(): String = "disconnect"

    override def execCmd(args: String): Unit = {
        args split(" ") foreach {
            name => localActor ! RemoteMessages.Disconnect(name)
        }
    }
}
