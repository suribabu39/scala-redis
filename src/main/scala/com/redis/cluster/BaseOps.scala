package com.redis.cluster

import com.redis.RedisClientPool
import com.redis.api.BaseApi
import com.redis.serialization.{Format, Parse}

trait BaseOps extends BaseApi {
  rc: RedisClusterOps =>

  override def keys[A](pattern: Any)(implicit format: Format, parse: Parse[A]): Option[List[Option[A]]] = Some {
    onAllConns(_.keys[A](pattern))
      .foldLeft(List.empty[Option[A]]) {
        case (acc, el) => el match {
          case Some(x) => x ::: acc
          case None => acc
        }
      }
  }

  override def rename(oldkey: Any, newkey: Any)(implicit format: Format): Boolean = {
    val oldNode: RedisClientPool = nodeForKey(oldkey)
    val newNode: RedisClientPool = nodeForKey(newkey)
    if (oldNode == newNode) {
      oldNode.withClient(_.rename(oldkey, newkey))
    } else if (oldNode.withClient(_.exists(oldkey))) {
      val value = oldNode.withClient(_.get(oldkey))
      oldNode.withClient(_.del(oldkey))
      newNode.withClient(_.set(newkey, value))
    } else {
      throw new RuntimeException("ERR no such key")
    }
  }

  override def renamenx(oldkey: Any, newkey: Any)(implicit format: Format): Boolean = {
    val oldNode: RedisClientPool = nodeForKey(oldkey)
    val newNode: RedisClientPool = nodeForKey(newkey)
    if (oldNode == newNode) {
      oldNode.withClient(_.renamenx(oldkey, newkey))
    } else if (oldNode.withClient(_.exists(oldkey))) {
      if (newNode.withClient(_.exists(newkey))) {
        false
      } else {
        val value = oldNode.withClient(_.get(oldkey))
        oldNode.withClient(_.del(oldkey))
        newNode.withClient(_.set(newkey, value))
      }
    } else {
      throw new RuntimeException("ERR no such key")
    }
  }

  override def dbsize: Option[Long] = {
    val r = onAllConns(_.dbsize).flatten
    if (r.isEmpty) None else Some(r.sum)
  }

  override def exists(key: Any)(implicit format: Format): Boolean =
    processForKey(key)(_.exists(key))

  override def vexists(key: Any, keys: Any*)(implicit format: Format): Option[Long] =
    processForKey(key)(_.vexists(key, keys: _*))

  override def del(key: Any, keys: Any*)(implicit format: Format): Option[Long] = Some {
    (key :: keys.toList)
      .groupBy(nodeForKey)
      .foldLeft(0L) { case (t, (n, ks)) =>
        n.withClient { client =>
          client.del(ks.head, ks.tail: _*).map(t +).getOrElse(t)
        }
      }
  }

  override def getType(key: Any)(implicit format: Format): Option[String] =
    processForKey(key)(_.getType(key))

  override def expire(key: Any, ttl: Int)(implicit format: Format): Boolean =
    processForKey(key)(_.expire(key, ttl))

  override def pexpire(key: Any, ttlInMillis: Int)(implicit format: Format): Boolean =
    processForKey(key)(_.pexpire(key, ttlInMillis))

  override def expireat(key: Any, timestamp: Long)(implicit format: Format): Boolean =
    processForKey(key)(_.expireat(key, timestamp))

  override def pexpireat(key: Any, timestampInMillis: Long)(implicit format: Format): Boolean =
    processForKey(key)(_.pexpireat(key, timestampInMillis))

  override def ttl(key: Any)(implicit format: Format): Option[Long] =
    processForKey(key)(_.ttl(key))

  override def pttl(key: Any)(implicit format: Format): Option[Long] =
    processForKey(key)(_.pttl(key))

  override def flushdb: Boolean =
    onAllConns(_.flushdb) forall (_ == true)

  override def flushall: Boolean =
    onAllConns(_.flushall) forall (_ == true)

  override def quit: Boolean =
    onAllConns(_.quit) forall (_ == true)

  override def time[A](implicit format: Format, parse: Parse[A]): Option[List[Option[A]]] =
    randomNode().withClient(_.time)

  override def randomkey[A](implicit parse: Parse[A]): Option[A] =
    onAllConns(_.randomkey).flatten.headOption

  override def select(index: Int): Boolean =
    onAllConns(_.select(index)).forall(_ == true)

  override def move(key: Any, db: Int)(implicit format: Format): Boolean =
    processForKey(key)(_.move(key, db))

  override def auth(secret: Any)(implicit format: Format): Boolean =
    onAllConns(_.auth(secret)).forall(_ == true)

  override def persist(key: Any)(implicit format: Format): Boolean =
    processForKey(key)(_.persist(key))

  // todo: implement
  override def scan[A](cursor: Int, pattern: Any, count: Int)
                      (implicit format: Format, parse: Parse[A]): Option[(Option[Int], Option[List[Option[A]]])] = ???

  override def ping: Option[String] =
    if (onAllConns(_.ping).forall(_ == pong)) {
      pong
    } else {
      None
    }

  // todo: implement
  override def watch(key: Any, keys: Any*)(implicit format: Format): Boolean = ???

  // todo: implement
  override def unwatch(): Boolean = ???

  // todo: implement
  override def getConfig(key: Any)(implicit format: Format): Option[Map[String, Option[String]]] = ???

  // todo: implement
  override def setConfig(key: Any, value: Any)(implicit format: Format): Option[String] = ???

  // todo: implement
  override def sort[A](key: String, limit: Option[(Int, Int)], desc: Boolean, alpha: Boolean, by: Option[String], get: List[String])(implicit format: Format, parse: Parse[A]): Option[List[Option[A]]] = ???

  // todo: implement
  override def sortNStore[A](key: String, limit: Option[(Int, Int)], desc: Boolean, alpha: Boolean, by: Option[String], get: List[String], storeAt: String)(implicit format: Format, parse: Parse[A]): Option[Long] = ???
}
