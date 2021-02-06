package com.redis.cluster

import com.redis.api.StringApi
import com.redis.api.StringApi.{Always, SetBehaviour}
import com.redis.serialization.{Format, Parse}

import scala.concurrent.duration.Duration

trait StringOps extends StringApi {
  rc: RedisClusterOps =>

  override def set(key: Any, value: Any, whenSet: SetBehaviour = Always, expire: Duration = null)
                  (implicit format: Format): Boolean =
    processForKey(key)(_.set(key, value, whenSet, expire))

  override def get[A](key: Any)(implicit format: Format, parse: Parse[A]): Option[A] =
    processForKey(key)(_.get(key))


}
