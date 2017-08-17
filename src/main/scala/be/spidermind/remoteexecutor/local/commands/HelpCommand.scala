package be.spidermind.remoteexecutor.local.commands

import be.spidermind.remoteexecutor.annotations.{CommandLine, CommandLineHelp}
import be.spidermind.remoteexecutor.local.commands.types.CommandLineHandler
import be.spidermind.remoteexecutor.local.interpreter.Interpreter

/**
  * Created by anonyme77 on 16/08/2017.
  */
@CommandLine
@CommandLineHelp(cmd = "help",
    example="help ls ",
    explanation = "Prints this helper")
class HelpCommand extends CommandLineHandler {
    override def cmdKey(): String = "help"

    override def execCmd(line: String): Unit = {
        println("###########################################")
        println("--------> Commandes")
        println("###########################################")
        Interpreter.commandLineClassesInfo().foreach {
            classe =>
                classe.annotations
                .filter(_.descriptor.endsWith("CommandLineHelp;"))
                    .foreach {
                        a =>
                            println("\n\n---> "+a.params("cmd").asInstanceOf[String])
                            println("--------> Explication : "+a.params("explanation").asInstanceOf[String])
                            println("--------> Exemple : "+a.params("example").asInstanceOf[String])
                    }
        }

    }

}

