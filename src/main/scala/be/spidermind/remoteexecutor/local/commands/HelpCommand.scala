package be.spidermind.remoteexecutor.local.commands

import be.spidermind.remoteexecutor.local.commands.types.{CommandHelper, CommandLineHandler}
import be.spidermind.remoteexecutor.local.interpreter.Interpreter

/**
  * Created by anonyme77 on 16/08/2017.
  */
class HelpCommand extends CommandLineHandler {
    override def cmdKey(): String = "help"

    override def execCmd(line: String): Unit = {
        println("###########################################")
        println("--------> Commandes")
        println("###########################################")
        Interpreter.cmdLineMap().values.foreach{
            cmd =>
                val helper = cmd.help()
                            println("\n\n---> "+helper.getCmd())
                            println("--------> Explication : "+helper.getExplanation())
                            println("--------> Exemple : "+helper.getExample())
                    }

        println("\n\n###########################################")
        println("--------> Directed Commandes")
        println("###########################################")
        Interpreter.directedCmdLineMap().values.foreach{
            cmd =>
                val helper = cmd.help()
                println("\n\n---> "+helper.getCmd())
                println("--------> Explication : "+helper.getExplanation())
                println("--------> Exemple : "+helper.getExample())
        }

    }

    override def help(): CommandHelper =
        new CommandHelper("help",
            "Prints this helper",
            "help ls"
        )

}

