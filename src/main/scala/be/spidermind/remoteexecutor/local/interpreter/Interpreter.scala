package be.spidermind.remoteexecutor.local.interpreter

import be.spidermind.remoteexecutor.local.commands.types.CommandLineHandler
import org.clapper.classutil.{ClassFinder, ClassInfo}

/**
  * Created by anonyme77 on 17/08/2017.
  */

object Interpreter extends Interpreter {
    val knownCommands = cmdLineObjectsMap.keySet

    def executeCommand(cmd:String, args:String) = {
        cmdLineObjectsMap()(cmd).execute(args)
    }

    def main(args: Array[String]): Unit = {
        println(knownCommands)
    }

}

class Interpreter {

    private var functionsMap: scala.collection.mutable.HashMap[String, CommandLineHandler] = null

    def commandLineClassesInfo():Stream[ClassInfo] = {
        val finder = ClassFinder()
        finder.getClasses
            .filter(_.isConcrete)
            .filter({
                classe =>
                    var found = false
                    classe.annotations.foreach {
                        annotation =>
                            println(annotation.descriptor)
                            if(annotation.descriptor.endsWith("CommandLine;"))
                                found = true

                    }
                    found
            })
    }

    def cmdLineObjectsMap() = {
        if(functionsMap == null) {
            functionsMap = scala.collection.mutable.HashMap[String, CommandLineHandler]()
            commandLineClassesInfo().foreach {
                classe =>
                    val theClass = Class.forName(classe.name)
                    val theObject: CommandLineHandler = theClass.newInstance().asInstanceOf[CommandLineHandler]
                    functionsMap.put(theObject.cmdKey(), theObject)
            }
        }

        functionsMap
    }

}
