package be.spidermind.remoteexecutor.local.commands.types

import be.spidermind.remoteexecutor.RemoteMessages

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

/**
  * Created by anonyme77 on 16/08/2017.
  */
abstract class CommandLineHandler extends Command {

    def chain(line:String):Future[Any] = {
        Future {
            execCmd(line)
        }
    }

    def execute(line:String):Unit = {
        val execution = Future {
            execCmd(line)
        }

        execution.onComplete {
            case Failure(e) => localActor!RemoteMessages.ExecutionResult("Command line ["+cmdKey()+" "+line
                +"] failed to execute ["+e.getMessage+"]")
            case _ =>
        }
    }

    def execCmd(line:String):Unit
}
