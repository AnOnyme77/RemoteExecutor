package be.spidermind.remoteexecutor.local.commands

import be.spidermind.remoteexecutor.annotations.{CommandLine, CommandLineHelp}
import be.spidermind.remoteexecutor.local.commands.types.CommandLineHandler
import be.spidermind.remoteexecutor.local.interpreter.Interpreter
import org.clapper.classutil.MethodInfo

/**
  * Created by anonyme77 on 17/08/2017.
  */

@CommandLine
@CommandLineHelp(cmd = "script",
    example = "script file.re",
    explanation = "executes the script /home/script.re. This script must contain commands accepted by this command line. It works like an interpreter. Script files can containt script command :)")
class ScriptCommand extends CommandLineHandler {
    override def cmdKey(): String = "script"

    override def execCmd(file: String): Unit = {
        scala.io.Source.fromFile(file).getLines().foreach {
            l => Interpreter.cmdLineObjectsMap()(l.split(" ").head).execute(l)
        }
    }
}
