package be.spidermind.remoteexecutor.local.commands.types

/**
  * Created by anonyme77 on 18/08/2017.
  */
class CommandHelper(cmd:String, explanation:String, example:String) {
    def getCmd() = cmd
    def getExplanation() = explanation
    def getExample() = example
}
