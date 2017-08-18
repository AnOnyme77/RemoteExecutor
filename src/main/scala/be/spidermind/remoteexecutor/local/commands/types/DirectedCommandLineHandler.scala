package be.spidermind.remoteexecutor.local.commands.types

import akka.actor.ActorRef
import be.spidermind.remoteexecutor.RemoteMessages

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

/**
  * Created by anonyme77 on 18/08/2017.
  */
abstract class DirectedCommandLineHandler extends Command {

    def chain(remote:String, line:String):Future[Any] = {
        Future {
            execCmd(remote, line)
        }
    }

    def execute(remote:String, line:String):Unit = {
        val execution = Future {
            execCmd(remote, line)
        }

        execution.onComplete {
            case Failure(e) => localActor!RemoteMessages.ExecutionResult("Command line ["+cmdKey+" "+line
                +"] failed to execute ["+e.getMessage+"]")
            case _ =>
        }
    }

    def execCmd(remote:String, line:String):Unit
}
