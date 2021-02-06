package com.redis

import com.redis.api.StringApi
import com.redis.api.StringApi.{Always, SetBehaviour}
import com.redis.serialization._

import scala.concurrent.duration.Duration

trait StringOperations extends StringApi {
  self: Redis =>

  override def set(key: Any, value: Any, whenSet: SetBehaviour = Always, expire: Duration = null)
                  (implicit format: Format): Boolean = {
    val expireCmd = if (expire != null) {
      List("PX", expire.toMillis.toString)
    } else {
      List.empty
    }
    val cmd = List(key, value) ::: expireCmd ::: whenSet.command
    send("SET", cmd)(asBoolean)
  }

  override def get[A](key: Any)(implicit format: Format, parse: Parse[A]): Option[A] =
    send("GET", List(key))(asBulk)
}
