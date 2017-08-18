package be.spidermind.remoteexecutor.local.commands

import be.spidermind.remoteexecutor.RemoteMessages
import be.spidermind.remoteexecutor.local.commands.types.{CommandHelper, CommandLineHandler}

/**
  * Created by anonyme77 on 16/08/2017.
  */
class DisconnectCommand extends CommandLineHandler {
    override def cmdKey(): String = "disconnect"

    override def execCmd(args: String): Unit = {
        args split(" ") foreach {
            name => localActor ! RemoteMessages.Disconnect(name)
        }
    }

    override def help(): CommandHelper =
        new CommandHelper("disconnect",
            "disconnect computer identified by 'name'",
            "disconnect name"
        )
}
