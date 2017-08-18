package be.spidermind.remoteexecutor.local.commands

import be.spidermind.remoteexecutor.RemoteMessages
import be.spidermind.remoteexecutor.local.commands.types.{CommandHelper, CommandLineHandler}

/**
  * Created by anonyme77 on 16/08/2017.
  */
class ConnectedCommand extends CommandLineHandler {
    override def cmdKey(): String = "connected"

    override def execCmd(line: String): Unit = {
        localActor!RemoteMessages.Connected()
    }

    override def help(): CommandHelper =
        new CommandHelper("connected",
            "show all connected computers",
            "connected ls"
        )
}
