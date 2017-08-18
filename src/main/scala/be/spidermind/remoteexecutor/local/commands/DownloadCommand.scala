package be.spidermind.remoteexecutor.local.commands

import be.spidermind.remoteexecutor.RemoteMessages
import be.spidermind.remoteexecutor.local.commands.types.{CommandHelper, DirectedCommandLineHandler}

/**
  * Created by anonyme77 on 18/08/2017.
  */
class DownloadCommand extends DirectedCommandLineHandler {
    override def cmdKey(): String = "download"

    override def execCmd(remote: String, args: String): Unit = {
        val from = args.split(" ")(0)
        val to = args.split(" ")(1)

        localActor!RemoteMessages.Download(remote, from, to)
    }

    override def help(): CommandHelper =
        new CommandHelper("download",
            "do some nasty things",
            "bla bla"
        )
}
