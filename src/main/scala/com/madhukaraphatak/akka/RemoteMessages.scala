package com.madhukaraphatak.akka

/**
  * Created by anonyme77 on 25/07/2017.
  */
object RemoteMessages {
    case class Connect(name:String, ip:String, port:String)
    case class Disconnect(name:String)
    case class Connected()
    case class ExecCommand(f:String)
    case class ExecCommands(f:List[String])
    case class UploadFile(f:Array[Byte], destination:String)
    case class UploadEnd(destination:String)
    case class ExecutionResult(result:Any)
}
