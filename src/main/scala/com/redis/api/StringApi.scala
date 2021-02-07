package com.redis.api

//import com.redis.SecondsOrMillis
import com.redis.api.StringApi.{Always, NX, SetBehaviour, XX}
import com.redis.serialization.{Format, Parse}

import scala.concurrent.duration.Duration

trait StringApi {

  /**
    * sets the key with the specified value.
    * Starting with Redis 2.6.12 SET supports a set of options that modify its behavior:
    *
    * NX -- Only set the key if it does not already exist.
    * XX -- Only set the key if it already exist.
    * PX milliseconds -- Set the specified expire time, in milliseconds.
    */
  def set(key: Any, value: Any, whenSet: SetBehaviour = Always, expire: Duration = null)
         (implicit format: Format): Boolean

  /**
   * gets the value for the specified key.
   */
  def get[A](key: Any)(implicit format: Format, parse: Parse[A]): Option[A]
}

object StringApi {

  // @formatter:off
  sealed abstract class SetBehaviour(val command: List[String]) // singleton list
  case object NX     extends SetBehaviour(List("NX"))
  case object XX     extends SetBehaviour(List("XX"))
  case object Always extends SetBehaviour(List.empty)
  // @formatter:on

}
