package be.spidermind.remoteexecutor.local.interpreter

import be.spidermind.remoteexecutor.local.commands.types.{CommandLineHandler, DirectedCommandLineHandler}
import org.clapper.classutil.{ClassFinder, ClassInfo}

import scala.util.{Failure, Try}

/**
  * Created by anonyme77 on 17/08/2017.
  */

object Interpreter extends Interpreter {
    val knownCommands = cmdLineMap.keySet
    val knownDirectedCommands = directedCmdLineMap.keySet

    def execCommandLine(line:String) = {
        Try{
            val functionName = getFunction(line)
            if(functionName startsWith("#")) {
                val remote = functionName.replace("#","")
                val function = getFunction(getArgs(line))
                val args = getArgs(getArgs(line))

                if(!knownDirectedCommands.contains(function)) {
                    println("Fonction inconnue")
                } else {
                    executeDirectedCommand(remote,function,args)
                }

            } else if(knownCommands.contains(functionName)) {
                executeCommand(functionName,getArgs(line))
            } else {
                executeCommand("exec",line)
            }
        } match {
            case Failure(ex) => println("Une erreur est survenue : "+ex.getMessage)
            case _ =>
        }
    }

    private def getFunction(ln:String) = {
        ln.split(" ")(0)
    }

    private def getArgs(ln:String):String = {
        val words = ln split(" ")
        words.slice(1, words.length).mkString(" ")
    }

    private def executeCommand(cmd:String, args:String) = {
        cmdLineMap()(cmd).execute(args)
    }

    private def executeDirectedCommand(remote:String, cmd:String, args:String) = {
        directedCmdLineMap()(cmd).execute(remote, args)
    }
}

class Interpreter {

    private val functionsMap: scala.collection.mutable.HashMap[String, CommandLineHandler] =
            new scala.collection.mutable.HashMap[String, CommandLineHandler]()
    private val directedFunctionsMap: scala.collection.mutable.HashMap[String, DirectedCommandLineHandler] =
        new scala.collection.mutable.HashMap[String, DirectedCommandLineHandler]()

    def cmdLineMap() = {
        functionsMap
    }

    def directedCmdLineMap() = {
        directedFunctionsMap
    }

    def addCommand(cmd:String, commandLineHandler: CommandLineHandler):Unit = {
        functionsMap.put(cmd, commandLineHandler)
    }

    def addDirectedCommand(cmd:String, commandLineHandler: DirectedCommandLineHandler):Unit = {
        directedFunctionsMap.put(cmd, commandLineHandler)
    }

}
