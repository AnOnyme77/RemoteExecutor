package be.spidermind.remoteexecutor.local.commands

import be.spidermind.remoteexecutor.RemoteMessages
import be.spidermind.remoteexecutor.annotations.{CommandLine, CommandLineHelp}
import be.spidermind.remoteexecutor.local.commands.types.CommandLineHandler

/**
  * Created by anonyme77 on 16/08/2017.
  */
@CommandLine
@CommandLineHelp(cmd = "connected",
    example="connected ls",
    explanation = "show all connected computers")
class ConnectedCommand extends CommandLineHandler {
    override def cmdKey(): String = "connected"

    override def execCmd(line: String): Unit = {
        localActor!RemoteMessages.Connected()
    }
}
