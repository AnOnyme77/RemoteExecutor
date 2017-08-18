package be.spidermind.remoteexecutor.local.commands.types

import akka.actor.ActorRef
import be.spidermind.remoteexecutor.RemoteMessages

import scala.concurrent.ExecutionContext

/**
  * Created by anonyme77 on 18/08/2017.
  */
abstract class Command {

    protected var localActor:ActorRef = null

    protected implicit val ex:ExecutionContext = ExecutionContext.global

    def setRemotes(localActor:ActorRef) = {
        this.localActor = localActor
    }

    def outputString(line:String):Unit = {
        localActor!RemoteMessages.ExecutionResult(line)
    }

    def throwError(e:String) = {
        this.localActor!RemoteMessages.ExecutionResult("Exécution impossible ["+e+"]")
    }

    def cmdKey():String

    def help():CommandHelper
}
