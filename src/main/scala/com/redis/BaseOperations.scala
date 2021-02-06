package com.redis

import com.redis.api.BaseApi
import com.redis.serialization._

trait BaseOperations extends BaseApi {
  self: Redis =>

  override def expire(key: Any, ttl: Int)(implicit format: Format): Boolean =
    send("EXPIRE", List(key, ttl))(asBoolean)

  override def select(index: Int): Boolean =
    send("SELECT", List(index))(if (asBoolean) {
      db = index
      true
    } else {
      false
    })

  override def flushdb: Boolean =
    send("FLUSHDB")(asBoolean)

  override def flushall: Boolean =
    send("FLUSHALL")(asBoolean)

  override def quit: Boolean =
    send("QUIT")(disconnect)

  override def auth(secret: Any)(implicit format: Format): Boolean =
    send("AUTH", List(secret))(asBoolean)

  override def ping: Option[String] =
    send("PING")(asString)
}
