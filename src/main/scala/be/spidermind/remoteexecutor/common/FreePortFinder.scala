package be.spidermind.remoteexecutor.common

import java.net.ServerSocket

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * Created by anonyme77 on 19/08/2017.
  */
object FreePortFinder {

    private implicit val ex:ExecutionContext = ExecutionContext.global

    private var portRange = 9000 until 9100
    private var openPorts = scala.collection.mutable.ListBuffer[Int]()

    def beginPortsDiscovery(reverse:Boolean = false): Unit = {
        Future {
            findPorts(reverse)
        }
    }

    def getFreshPort(): Int = {
        val port = openPorts.remove(0)
        if(Parameters.debug) println("Took port "+port)
        if(Parameters.debug) println(openPorts.size+" ports left")
        port
    }

    private def findPorts(reverse:Boolean) = {
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
                        case Failure(e) =>
                            if(Parameters.debug) println("Error during port discovery")
                    }
                }
        }

        if(Parameters.debug)
            println("Port table size is now "+openPorts.size)
    }
}
