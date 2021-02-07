package com.redis.cluster

import com.typesafe.config.{Config, ConfigException, ConfigFactory, ConfigValueFactory}
import com.redis.{RedisClient, RedisConnectionException}
import com.redis.api.StringApi.{Always, SetBehaviour}
import com.redis.serialization.Format

import java.net.{SocketException, ConnectException}
import collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.util.Random


class RedisCluster {

  val nodePrefix = "node"
  private var nodeClusters: List[RedisClient] = _

  def intializeCluster: Unit = {
    println("RedisCluster: Initializing redis cluster ...")
    try {
      val config = ConfigFactory.load("application.conf")
      val connections: List[String] = config.getStringList("connections").asScala.toList
      println(s"Configured ${connections.length} connection(s)")
      nodeClusters = connections.zipWithIndex.map {
        case (url, idx) => {
          val urlSplit = url.split(":")
          val host = urlSplit(0)
          val port = urlSplit(1).toInt
          new RedisClient(s"$nodePrefix${idx+1}", host, port)
        }
      }
      println("RedisCluster: Available nodes ...")
      nodeClusters.foreach(node => println(s"${node.getName} - $node"))
      println("RedisCluster: Cluster has been configured")
    } catch {
      case ex: ConfigException =>
        println("RedisCluster: Could not find connection urls in application.conf")
        println(s"Example: connections=['127.0.0.1:7001','127.0.0.1:7002'','127.0.0.1:7003']")
        throw ex
    }
  }

  def set(nodeName: String, key: String, value: String, whenSet: SetBehaviour = Always, expire: Duration = null): Boolean = {
    val node = nodeClusters.find(_.getName == nodeName).getOrElse(getAnyNode)
    if(nodeName == node.getName || nodeName.isEmpty)
      println(s"RedisCluster: ${node.getName} is trying to SET $key - $value")
    else
      println(s"RedisCluster: Failed to use ${nodeName}. ${node.getName} is trying to SET $key - $value")
    try {
      node.set(key, value, whenSet, expire)
    } catch {
      case _: Throwable =>
        if(!node.connected) blacklistNode(node)
        false
    }
  }

  def get(nodeName: String, key: String): Option[String] = {
    val node = nodeClusters.find(_.getName == nodeName).getOrElse(getAnyNode)
    if(nodeName == node.getName || nodeName.isEmpty)
      println(s"RedisCluster: ${node.getName} is trying to GET $key")
    else
      println(s"RedisCluster: Failed to use ${nodeName}. ${node.getName} is trying to GET $key")
    try {
      node.get(key)
    } catch {
      case _: Throwable =>
        if(!node.connected) blacklistNode(node)
        None
    }
  }

  def expire(nodeName: String, key: String, ttl: Int): Boolean = {
    val node = nodeClusters.find(_.getName == nodeName).getOrElse(getAnyNode)
    if(nodeName == node.getName || nodeName.isEmpty)
      println(s"RedisCluster: ${node.getName} is trying to set EXPIRE for $key")
    else
      println(s"RedisCluster: Failed to use ${nodeName}. ${node.getName} is trying to set EXPIRE for $key")
    try {
      node.expire(key, ttl)
    } catch {
      case _: Throwable =>
        if(!node.connected) blacklistNode(node)
        false
    }
  }

  def close() = {
    nodeClusters.foreach(_.close())
    println("RedisCluster: redis cluster is shutdown")
  }

  def getAnyNode: RedisClient = Random.shuffle(nodeClusters).head

  def blacklistNode(node: RedisClient) = {
    if (nodeClusters.map(_.getName).contains(node.getName)) {
      println(s"RedisCluster: blacklisting node - ${node.getName}[$node]")
      try {
        node.ping match {
          case Some("PONG") =>
            println(s"${node} is still live")
          case Some(_) =>
            println(s"{node.getName} - Invalid response")
          case None =>
            println(s"${node.getName} is disconnected")
            nodeClusters = nodeClusters.filter(_.getName != node.getName)
        }
      } catch {
        case _: ConnectException =>
        case ex: Throwable =>
          ex.getCause match {
            case _: ConnectException =>
              println(s"RedisCluster: Connection closed for ${node.getName} - $node")
              nodeClusters = nodeClusters.filter(_.getName != node.getName)
            case _ =>
              ex.printStackTrace()
              println(s"RedisCluster: Unknown exception occurred while blacklisting ${node.getName} - $node")
          }
      }
      println(s"RedisCluster: Num of available nodes - ${nodeClusters.size}")
    } else {
      println(s"RedisCluster: ${node.getName}[$node] is already blacklisted or unavailable")
    }
  }
}
