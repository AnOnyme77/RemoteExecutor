package be.spidermind.remoteexecutor.local.commands

import be.spidermind.remoteexecutor.local.commands.types.{CommandHelper, CommandLineHandler}
import be.spidermind.remoteexecutor.local.interpreter.Interpreter
import org.clapper.classutil.MethodInfo

/**
  * Created by anonyme77 on 17/08/2017.
  */

class ScriptCommand extends CommandLineHandler {
    override def cmdKey(): String = "script"

    override def execCmd(file: String): Unit = {
        scala.io.Source.fromFile(file).getLines().foreach {
            l => Interpreter.cmdLineMap()(l.split(" ").head).execute(l)
        }
    }

    override def help(): CommandHelper =
        new CommandHelper("script",
            "executes the script /home/script.re. This script must contain commands accepted by this command line. It works like an interpreter. Script files can containt script command :)",
            "script file.re"
        )
}
