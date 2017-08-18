package be.spidermind.remoteexecutor.local.commands

import be.spidermind.remoteexecutor.local.commands.types.{CommandHelper, CommandLineHandler}

import scala.util.{Failure, Success}

/**
  * Created by anonyme77 on 16/08/2017.
  */
class PythonCommand extends CommandLineHandler {
    override def cmdKey(): String = "python"

    override def execCmd(args: String): Unit = {
        val to = args.split(" ")(1)
        val uploadCommand = new UploadCommand()
        uploadCommand.setRemotes(localActor)
        val execPythonCommand = new ExecCommand()
        execPythonCommand.setRemotes(localActor)
        val execRmCommand = new ExecCommand()
        execRmCommand.setRemotes(localActor)


        uploadCommand.chain(args).map {
            _ => execPythonCommand.execute("python "+to)
        }.map {
                _ => execRmCommand.execute("rm "+to)
        }.onComplete {
            case Failure(e) => throwError("Erreur : "+e.getMessage)
            case _ =>
        }
    }

    override def help(): CommandHelper =
        new CommandHelper("python",
            "upload 'from' python script to all computers on 'to' file path and execute the script with python on all connected computers",
            "python from to"
        )
}
