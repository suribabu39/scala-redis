package com.redis

import com.redis.api.BaseApi
import com.redis.serialization._

trait BaseOperations extends BaseApi {
  self: Redis =>

  override def expire(key: Any, ttl: Int)(implicit format: Format): Boolean =
    send("EXPIRE", List(key, ttl))(asBoolean)

  override def flushall: Boolean =
    send("FLUSHALL")(asBoolean)

  override def quit: Boolean =
    send("QUIT")(disconnect)

  override def ping: Option[String] =
    send("PING")(asString)
}
