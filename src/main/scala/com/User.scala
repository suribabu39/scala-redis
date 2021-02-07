package com

import com.redis.RedisConnectionException
import com.redis.cluster.RedisCluster

import java.net.{SocketException, ConnectException}
import scala.io.Source

class User(userId: Int, filePath: String) extends Runnable {

  override def run(): Unit = {
    println(s"User[$userId]: Reading $filePath")
    val bufferedSource = Source.fromFile(filePath)
    lazy val redisCluster = new RedisCluster()
    try {
      var node = "node1"
      println(s"User[$userId]: intializing redis cluster")
      redisCluster.intializeCluster
      for (line <- bufferedSource.getLines) {
        val lineArr = line.split(" ")
        println(s"User[$userId]: $line")
        if (lineArr.head.equalsIgnoreCase("use") && lineArr.length == 2) {
          val newNode = lineArr(1)
          if(!newNode.startsWith("node")) {
            println(s"User[$userId]: Invalid node - $newNode")
          } else {
            node = newNode
          }
        } else {
          lineArr.head.toLowerCase match {
            case "set" => {
              assert(lineArr.length == 3, "Invalid SET statement")
              val result = redisCluster.set(node, lineArr(1), lineArr(2))
              result match {
                case true => println(s"User[$userId]: Added key - ${lineArr(1)}")
                case false => println(s"User[$userId]: Could not add key - ${lineArr(1)}")
              }
            }
            case "get" => {
              assert(lineArr.length == 2, "Invalid GET statement")
              val result = redisCluster.get(node, lineArr(1))
              result match {
                case Some(value) => println(s"User[$userId]: Got $value key - ${lineArr(1)}")
                case None => println(s"User[$userId]: Failed to get value for key - ${lineArr(1)}")
              }
            }
            case "expire" => {
              assert(lineArr.length == 3, "Invalid EXPIRE statement")
              val result = redisCluster.expire(node, lineArr(1), lineArr(2).toInt)
              result match {
                case true => println(s"User[$userId]: Set expiry for key - ${lineArr(1)}")
                case false => println(s"User[$userId]: Could not set expiry key - ${lineArr(1)}")
              }
            }
            case _ =>
          }
        }
      }
      println(s"User[$userId]: Executed all the commands successfully")
    } catch {
      case _: RedisConnectionException => println(s"User[$userId]: Redis connection exception occurred")
      case _: SocketException => println(s"User[$userId]: Socket closed exception occurred")
      case _: ConnectException => println(s"User[$userId]: Could not connect to redis cluster")
      case ex: Exception => println(s"User[$userId]: unknown exception(${ex.getMessage}) occurred")
    } finally {
      println(s"User[$userId]: shutting down redis cluster ")
      redisCluster.close()
    }
  }
}
