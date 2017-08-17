package be.spidermind.remoteexecutor.local.commands

import java.nio.file.{Files, Paths}

import be.spidermind.remoteexecutor.RemoteMessages
import be.spidermind.remoteexecutor.annotations.{CommandLine, CommandLineHelp}
import be.spidermind.remoteexecutor.local.commands.types.CommandLineHandler

/**
  * Created by anonyme77 on 16/08/2017.
  */
@CommandLine
@CommandLineHelp(cmd = "upload",
    example="upload from to",
    explanation = "upload 'from' local file to 'to' file path on all connected computers")
class UploadCommand extends CommandLineHandler {
    override def cmdKey(): String = "upload"

    override def execCmd(args: String): Unit = {
        val from = args.split(" ")(0)
        val to = args.split(" ")(1)
        val byteArray = Files.readAllBytes(Paths.get(from))
        localActor!RemoteMessages.UploadFile(byteArray,to)
    }
}
