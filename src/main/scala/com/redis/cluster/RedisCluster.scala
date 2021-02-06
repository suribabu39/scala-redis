package com.redis.cluster

import com.redis._
import com.redis.serialization._

import scala.util.{Failure, Success, Try}


class RedisCluster(
                    protected val hosts: List[ClusterNode],
                    override protected val keyTag: Option[KeyTag]
                  )
  extends RedisClusterOps
    with WithHashRing[IdentifiableRedisClientPool]
    with BaseOps
    with StringOps {

  // instantiating a cluster will automatically connect participating nodes to the server
  protected[cluster] val clients: List[IdentifiableRedisClientPool] = hosts.map { h =>
    new IdentifiableRedisClientPool(h)
  }

  // the hash ring will instantiate with the nodes up and added
  override protected[cluster] val hr: HashRing[IdentifiableRedisClientPool] =
    HashRing[IdentifiableRedisClientPool](clients, POINTS_PER_SERVER)

  override def nodeForKey(key: Any)(implicit format: Format): IdentifiableRedisClientPool = {
    val bKey = format(key)
    hr.getNode(keyTag.flatMap(_.tag(bKey.toIndexedSeq)).getOrElse(bKey.toIndexedSeq))
  }

  override def addServer(server: ClusterNode): Try[Unit] = Try {
    val instance = (new IdentifiableRedisClientPool(server))
    if (instance.withClient(_.ping) == pong) {
      Success(instance)
    } else {
      Failure(new Throwable(s"Ping method failed for $server"))
    }
  }.flatten.map { i => hr.addNode(i) }

  override def replaceServer(server: ClusterNode): Unit = {
    hr replaceNode new IdentifiableRedisClientPool(server) match {
      case Some(clientPool) => clientPool.close()
      case None =>
    }
  }

  override def removeServer(nodename: String): Unit =
    hr.cluster.find(_.node.nodename.equals(nodename)) match {
      case Some(pool) =>
        hr.removeNode(pool)
        Try(pool.close())
      case None =>
    }

  override def listServers: List[ClusterNode] = {
    hr.cluster.map(_.node).toList
  }

  override protected[cluster] def onAllConns[T](body: RedisClient => T): Iterable[T] =
    hr.cluster.map(p => p.withClient { client => body(client) })

  override def close(): Unit =
    hr.cluster.foreach(_.close())

  override protected[cluster] def randomNode(): RedisClientPool = {
    val rni = r.nextInt(hr.cluster.size)
    hr.cluster(rni)
  }

}
