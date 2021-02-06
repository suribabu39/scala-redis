package com.redis

import serialization.Parse
import Parse.{Implicits => Parsers}

private [redis] object Commands {

  // Response codes from the Redis server
  val ERR    = '-'
  val OK     = "OK".getBytes("UTF-8")
  val QUEUED = "QUEUED".getBytes("UTF-8")
  val SINGLE = '+'
  val BULK   = '$'
  val MULTI  = '*'
  val INT    = ':'

  val LS     = "\r\n".getBytes("UTF-8")

  def multiBulk(args: Seq[Array[Byte]]): Array[Byte] = {
    val b = new scala.collection.mutable.ArrayBuilder.ofByte
    b ++= "*%d".format(args.size).getBytes
    b ++= LS
    args foreach { arg =>
      b ++= "$%d".format(arg.size).getBytes
      b ++= LS
      b ++= arg
      b ++= LS
    }
    b.result
  }
}

import Commands._

case class RedisConnectionException(message: String) extends RuntimeException(message)
case class RedisMultiExecException(message: String) extends RuntimeException(message)

private [redis] trait Reply {

  type Reply[T] = PartialFunction[(Char, Array[Byte]), T]
  type SingleReply = Reply[Option[Array[Byte]]]

  def readLine: Array[Byte]
  def readCounted(c: Int): Array[Byte]

  val integerReply: Reply[Option[Int]] = {
    case (INT, s) => Some(Parsers.parseInt(s))
    case (BULK, s) if Parsers.parseInt(s) == -1 => None
  }

  val singleLineReply: SingleReply = {
    case (SINGLE, s) => Some(s)
    case (INT, s) => Some(s)
  }

  def bulkRead(s: Array[Byte]): Option[Array[Byte]] =
    Parsers.parseInt(s) match {
      case -1 => None
      case l =>
        val str = readCounted(l)
        val _ = readLine // trailing newline
        Some(str)
    }

  val bulkReply: SingleReply = {
    case (BULK, s) =>
      bulkRead(s)
  }

  val errReply: Reply[Nothing] = {
    case (ERR, s) => throw new Exception(Parsers.parseString(s))
    case x => throw new Exception("Protocol error: Got " + x + " as initial reply byte")
  }

  def receive[T](pf: Reply[T]): T = readLine match {
    case null => 
      throw new RedisConnectionException("Connection dropped ..")
    case line =>
      (pf orElse errReply) apply ((line(0).toChar,line.slice(1,line.length)))
  }

}

private [redis] trait R extends Reply {
  def asString: Option[String] = receive(singleLineReply) map Parsers.parseString

  def asBulk[T](implicit parse: Parse[T]): Option[T] =  receive(bulkReply) map parse

  def asBoolean: Boolean = receive(integerReply orElse singleLineReply) match {
    case Some(n: Int) => n > 0
    case Some(s: Array[Byte]) => Parsers.parseString(s) match {
      case "OK" => true
      case "QUEUED" => true
      case _ => false
    }
    case _ => false
  }
}

trait Protocol extends R