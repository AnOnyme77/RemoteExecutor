package be.spidermind.remoteexecutor.local.commands

import be.spidermind.remoteexecutor.RemoteMessages.LoadBalance
import be.spidermind.remoteexecutor.annotations.{CommandLine, CommandLineHelp}
import be.spidermind.remoteexecutor.local.commands.types.CommandLineHandler

/**
  * Created by anonyme77 on 16/08/2017.
  */
@CommandLine
@CommandLineHelp(cmd = "balance",
    example="balance name producer:/path/to/script:function1 consumer:/path/to/script:function2",
    explanation = "create a system with name 'name' where we execute function1 (this function must have a single argument that is a queue where you have to put your elements) from producer script and give its outputs to function2 (this function must then have a single argument that is the element that is given to the function) on consumer script")
class BalanceCommand extends CommandLineHandler {
    override def cmdKey(): String = "balance"

    override def execCmd(args: String): Unit = {
        val majorParts = args.split(" ")
        val name = majorParts(0)
        val producerPart = majorParts(1).split(":")
        val producerScript = producerPart(1)
        val producerFunc = producerPart(2)
        val consumerPart = majorParts(2).split(":")
        val consumerScript = consumerPart(1)
        val consumerFunc = consumerPart(2)

        new UploadCommand().execute(consumerScript+" "+consumerScript.split("/").last)

        localActor!LoadBalance(name, producerScript, producerFunc,
            consumerScript, consumerFunc)
    }
}
