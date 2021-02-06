package com.redis

import java.net.SocketException
import javax.net.ssl.SSLContext

import com.redis.serialization.Format

object RedisClient {

}

trait Redis extends IO with Protocol {

  def send[A](command: String, args: Seq[Any])(result: => A)(implicit format: Format): A = try {
    write(Commands.multiBulk(command.getBytes("UTF-8") +: (args map (format.apply))))
    result
  } catch {
    case e: RedisConnectionException =>
      if (disconnect) send(command, args)(result)
      else throw e
    case e: SocketException =>
      if (disconnect) send(command, args)(result)
      else throw e
  }

  def send[A](command: String)(result: => A): A = try {
    write(Commands.multiBulk(List(command.getBytes("UTF-8"))))
    result
  } catch {
    case e: RedisConnectionException =>
      if (disconnect) send(command)(result)
      else throw e
    case e: SocketException =>
      if (disconnect) send(command)(result)
      else throw e
  }
}

trait RedisCommand extends Redis
  with BaseOperations
  with StringOperations
  with AutoCloseable {

  val database: Int = 0
  val secret: Option[Any] = None

  override def onConnect: Unit = {}

}


class RedisClient(override val host: String, override val port: Int,
    override val database: Int = 0, override val secret: Option[Any] = None, override val timeout : Int = 0, override val sslContext: Option[SSLContext] = None)
  extends RedisCommand {

  def this() = this("localhost", 6379)
  def this(connectionUri: java.net.URI) = this(
    host = connectionUri.getHost,
    port = connectionUri.getPort
  )
  override def toString: String = host + ":" + String.valueOf(port) + "/" + database

  override def close(): Unit = disconnect
}
