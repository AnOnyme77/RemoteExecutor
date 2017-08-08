package com.madhukaraphatak.akka

/**
  * Created by anonyme77 on 25/07/2017.
  */
object RemoteMessages {
    case class LoadBalance(name:String, producerScript:String, producerFunction: String,
                           consumerScript:String, consumerFunction:String)
    case class RemoteProcessResult(name:String, result:Any)
    case class TerminateProcess(name:String)
    case class AddInputToProcess(name:String, line:String)
    case class SpawnProcess(name:String, fileName:String, function:String)
    case class Connect(name:String, ip:String, port:String)
    case class Disconnect(name:String)
    case class Connected()
    case class ExecCommand(f:String)
    case class ExecCommands(f:List[String])
    case class UploadFile(f:Array[Byte], destination:String)
    case class UploadEnd(destination:String)
    case class DownloadEnd(path:String)
    case class DownloadData(data:Array[Byte], path:String)
    case class DownloadStart(path:String)
    case class Download(remote:String, from:String, to:String)
    case class ExecutionResult(result:Any)
}
