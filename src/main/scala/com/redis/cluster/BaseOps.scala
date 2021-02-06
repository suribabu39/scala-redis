package com.redis.cluster

import com.redis.RedisClientPool
import com.redis.api.BaseApi
import com.redis.serialization.{Format, Parse}

trait BaseOps extends BaseApi {
  rc: RedisClusterOps =>

  override def expire(key: Any, ttl: Int)(implicit format: Format): Boolean =
    processForKey(key)(_.expire(key, ttl))

  override def flushdb: Boolean =
    onAllConns(_.flushdb) forall (_ == true)

  override def flushall: Boolean =
    onAllConns(_.flushall) forall (_ == true)

  override def quit: Boolean =
    onAllConns(_.quit) forall (_ == true)

  override def auth(secret: Any)(implicit format: Format): Boolean =
    onAllConns(_.auth(secret)).forall(_ == true)

  override def ping: Option[String] =
    if (onAllConns(_.ping).forall(_ == pong)) {
      pong
    } else {
      None
    }

  override def select(index: Int): Boolean =
    onAllConns(_.select(index)).forall(_ == true)
}
