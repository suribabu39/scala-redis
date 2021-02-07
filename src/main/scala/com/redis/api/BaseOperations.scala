package com.redis.api

import com.redis.Redis
import com.redis.serialization.Format

trait BaseOperations extends BaseApi {
  self: Redis =>

  override def expire(key: Any, ttl: Int)(implicit format: Format): Boolean =
    send("EXPIRE", Some(List(key, ttl)))(asBoolean)

  override def flushall: Boolean =
    send("FLUSHALL")(asBoolean)

  override def quit: Boolean =
    send("QUIT")(disconnect)

  override def ping: Option[String] =
    send("PING")(asString)
}
