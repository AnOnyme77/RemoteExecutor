package be.spidermind.remoteexecutor.local.commands

import be.spidermind.remoteexecutor.RemoteMessages
import be.spidermind.remoteexecutor.annotations.{CommandLine, CommandLineHelp}
import be.spidermind.remoteexecutor.local.commands.types.CommandLineHandler

/**
  * Created by anonyme77 on 16/08/2017.
  */
@CommandLine
@CommandLineHelp(cmd = "connect",
    example="connect name 127.0.0.1 5150",
    explanation = "connect computer on address 127.0.0.1 on port 5150 with name 'name'")
class ConnectCommand extends CommandLineHandler {
    override def cmdKey(): String = "connect"

    override def execCmd(argsString: String): Unit = {
        val args = argsString split(" ")
        val name = args(0)
        val ip = args(1)
        val port = args(2)

        localActor!RemoteMessages.Connect(name, ip, port)
    }
}
