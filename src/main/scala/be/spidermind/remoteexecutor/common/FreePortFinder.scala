package be.spidermind.remoteexecutor.common

import java.net.ServerSocket

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

/**
  * Created by anonyme77 on 19/08/2017.
  */
object FreePortFinder {

    private implicit val ex:ExecutionContext = ExecutionContext.global

    private var portRange = 9000 until 9100
    private var openPorts = scala.collection.mutable.ListBuffer[Int]()

    def beginPortsDiscovery(reverse:Boolean = false): Unit = {
        Future {
            if(reverse) {
                portRange = portRange.reverse
            }

            portRange.foreach {
                port =>
                    Future {
                        Try {
                            new ServerSocket(port)
                        } match {
                            case Success(s) =>
                                if(Parameters.debug) println("port "+port+" is open")
                                s.close()
                                openPorts.append(port)
                            case _ =>
                        }
                    }
            }
        }
    }

    def getFreshPort(): Int = {
        val port = openPorts.remove(0)
        if(Parameters.debug) println("Took port "+port)
        if(Parameters.debug) println(openPorts.size+" ports left")
        port
    }
}
