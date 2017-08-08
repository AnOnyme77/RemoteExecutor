package com.madhukaraphatak.akka

import akka.actor.Actor

/**
  * Created by anonyme77 on 07/08/2017.
  */
class WriterActor extends Actor {
    override def receive: Receive = {
        case RemoteMessages.ExecutionResult(message) =>
            println("\r"+message)
            print("\n\rremote> ")
        case RemoteMessages.RemoteProcessResult(name, message) =>
            println("\rbalance output from ["+name+"] : "+message)
            print("\n\rremote> ")
    }
}
