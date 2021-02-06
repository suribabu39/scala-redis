package com.redis

import java.net.SocketException
import javax.net.ssl.SSLContext

import com.redis.serialization.Format

object RedisClient {

  private def extractDatabaseNumber(connectionUri: java.net.URI): Int = {
    Option(connectionUri.getPath).map(path =>
      if (path.isEmpty) 0
      else Integer.parseInt(path.tail)
    )
      .getOrElse(0)
  }
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

  def cmd(args: Seq[Array[Byte]]): Array[Byte] = Commands.multiBulk(args)

  protected def flattenPairs(in: Iterable[Product2[Any, Any]]): List[Any] =
    in.iterator.flatMap(x => Iterator(x._1, x._2)).toList

}

trait RedisCommand extends Redis
  with BaseOperations
  with StringOperations
  with AutoCloseable {

  val database: Int = 0
  val secret: Option[Any] = None

  override def onConnect: Unit = {
    secret.foreach {s =>
      auth(s)
    }
    selectDatabase()
  }

  private def selectDatabase(): Unit = {
    if (database != 0)
      select(database)
  }

  private def authenticate(): Unit = {
    secret.foreach(auth _)
  }

}


class RedisClient(override val host: String, override val port: Int,
    override val database: Int = 0, override val secret: Option[Any] = None, override val timeout : Int = 0, override val sslContext: Option[SSLContext] = None)
  extends RedisCommand {

  def this() = this("localhost", 6379)
  def this(connectionUri: java.net.URI) = this(
    host = connectionUri.getHost,
    port = connectionUri.getPort,
    database = RedisClient.extractDatabaseNumber(connectionUri),
    secret = Option(connectionUri.getUserInfo)
      .flatMap(_.split(':') match {
        case Array(_, password, _*) => Some(password)
        case _ => None
      })
  )
  override def toString: String = host + ":" + String.valueOf(port) + "/" + database

  def pipeline(f: PipelineClient => Any): Option[List[Any]] = {
    send("MULTI")(asString) // flush reply stream
    try {
      val pipelineClient = new PipelineClient(this)
      try {
        f(pipelineClient)
      } catch {
        case e: Exception =>
          send("DISCARD")(asString)
          throw e
      }
      send("EXEC")(asExec(pipelineClient.handlers))
    } catch {
      case e: RedisMultiExecException =>
        None
    }
  }

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.{Future, Promise}
  import scala.util.Try

  /**
   * Redis pipelining API without the transaction semantics. The implementation has a non-blocking
   * semantics and returns a <tt>List</tt> of <tt>Promise</tt>. The caller may use <tt>Future.firstCompletedOf</tt> to get the
   * first completed task before all tasks have been completed.
   * If an exception is raised in executing any of the commands, then the corresponding <tt>Promise</tt> holds
   * the exception. Here's a sample usage:
   * <pre>
   * val x =
   *  r.pipelineNoMulti(
   *    List(
   *      {() => r.set("key", "debasish")},
   *      {() => r.get("key")},
   *      {() => r.get("key1")},
   *      {() => r.lpush("list", "maulindu")},
   *      {() => r.lpush("key", "maulindu")}     // should raise an exception
   *    )
   *  )
   * </pre>
   *
   * This queues up all commands and does pipelining. The returned r is a <tt>List</tt> of <tt>Promise</tt>. The client
   * may want to wait for all to complete using:
   *
   * <pre>
   * val result = x.map{a => Await.result(a.future, timeout)}
   * </pre>
   *
   * Or the client may wish to track and get the promises as soon as the underlying <tt>Future</tt> is completed.
   */
  def pipelineNoMulti(commands: Seq[() => Any]) = {
    val ps = List.fill(commands.size)(Promise[Any]())
    var i = -1
    val f = Future {
      commands.map {command =>
        i = i + 1
        Try {
          command()
        } recover {
          case ex: java.lang.Exception =>
            ps(i) success ex
        } foreach {r =>
          ps(i) success r
        }
      }
    }
    ps
  }

  class PipelineClient(parent: RedisClient) extends RedisCommand {
    import com.redis.serialization.Parse

    var handlers: Vector[() => Any] = Vector.empty

    override def send[A](command: String, args: Seq[Any])(result: => A)(implicit format: Format): A = {
      write(Commands.multiBulk(command.getBytes("UTF-8") +: (args map (format.apply))))
      handlers :+= (() => result)
      receive(singleLineReply).map(Parse.parseDefault)
      null.asInstanceOf[A] // ugh... gotta find a better way
    }
    override def send[A](command: String)(result: => A): A = {
      write(Commands.multiBulk(List(command.getBytes("UTF-8"))))
      handlers :+= (() => result)
      receive(singleLineReply).map(Parse.parseDefault)
      null.asInstanceOf[A]
    }

    val host = parent.host
    val port = parent.port
    val timeout = parent.timeout
    override val secret = parent.secret
    override val database = parent.database

    // TODO: Find a better abstraction
    override def connected = parent.connected
    override def connect = parent.connect
    override def disconnect = parent.disconnect
    override def clearFd = parent.clearFd
    override def write(data: Array[Byte]) = parent.write(data)
    override def readLine = parent.readLine
    override def readCounted(count: Int) = parent.readCounted(count)
    override def onConnect() = parent.onConnect()

    override def close(): Unit = parent.close()
  }

  override def close(): Unit = disconnect
}
