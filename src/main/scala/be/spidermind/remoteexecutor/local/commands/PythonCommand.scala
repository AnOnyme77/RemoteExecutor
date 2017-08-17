package be.spidermind.remoteexecutor.local.commands

import be.spidermind.remoteexecutor.annotations.{CommandLine, CommandLineHelp}
import be.spidermind.remoteexecutor.local.commands.types.CommandLineHandler

import scala.util.{Failure, Success}

/**
  * Created by anonyme77 on 16/08/2017.
  */
@CommandLine
@CommandLineHelp(cmd = "python",
    example="python from to",
    explanation = "upload 'from' python script to all computers on 'to' file path and execute the script with python on all connected computers")
class PythonCommand extends CommandLineHandler {
    override def cmdKey(): String = "python"

    override def execCmd(args: String): Unit = {
        val to = args.split(" ")(1)
        val uploadCommand = new UploadCommand()
        val execPythonCommand = new ExecCommand()
        val execRmCommand = new ExecCommand()


        uploadCommand.chain(args).map {
            _ => execPythonCommand.execute("python "+to)
        }.map {
                _ => execRmCommand.execute("rm "+to)
        }.onComplete {
            case Failure(e) => throwError("Erreur : "+e.getMessage)
            case _ =>
        }
    }
}
