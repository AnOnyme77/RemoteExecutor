package be.spidermind.remoteexecutor.local.commands

import be.spidermind.remoteexecutor.RemoteMessages
import be.spidermind.remoteexecutor.local.commands.types.{CommandHelper, CommandLineHandler}

/**
  * Created by anonyme77 on 16/08/2017.
  */
class ConnectCommand extends CommandLineHandler {
    override def cmdKey(): String = "connect"

    override def execCmd(argsString: String): Unit = {
        val args = argsString split(" ")
        val name = args(0)
        val ip = args(1)
        val port = args(2)

        localActor!RemoteMessages.Connect(name, ip, port)
    }

    override def help(): CommandHelper =
        new CommandHelper("connect",
            "connect computer on address 127.0.0.1 on port 5150 with name 'name'",
            "connect name 127.0.0.1 5150"
        )
}
