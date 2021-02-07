package com.redis

import com.redis.api.{BaseOperations, StringOperations}

import java.net.SocketException
import com.redis.serialization.Format


trait Redis extends IO with Protocol {

  def retry[A](command: String, extraArgs: Option[Seq[Any]] = None) (result: => A)(implicit format: Format): A = {
    extraArgs match {
      case Some(args) => send(command, Some(args))(result)
      case None => send(command)(result)
    }
  }

  def send[A](command: String, extraArgs: Option[Seq[Any]] = None)(result: => A)(implicit format: Format): A = {
    try {
      extraArgs match {
        case Some(args) =>
          write(Commands.multiBulk(command.getBytes("UTF-8") +: (args map (format.apply))))
          result
        case None =>
          write(Commands.multiBulk(List(command.getBytes("UTF-8"))))
          result
      }
    } catch {
      case e: RedisConnectionException =>
        if (disconnect) retry(command, extraArgs) (result) else throw e
      case e: SocketException =>
        if (disconnect) retry(command, extraArgs) (result) else throw e
      case e: Throwable =>
        throw e
    }
  }
}


class RedisClient(override val name: String, override val host: String, override val port: Int)
  extends Redis
  with BaseOperations
  with StringOperations
  with AutoCloseable {

  def getName = name
  val database: Int = 0
  val secret: Option[Any] = None
  override def onConnect: Unit = {}

  def this() = this("sample-node", "localhost", 6379)
  def this(connectionUri: java.net.URI) = this(
    name = "sample-node",
    host = connectionUri.getHost,
    port = connectionUri.getPort
  )
  override def toString: String = host + ":" + String.valueOf(port) + "/" + database

  override def close(): Unit = disconnect
}
