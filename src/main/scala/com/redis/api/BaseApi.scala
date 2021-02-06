package com.redis.api

import com.redis.serialization.{Format, Parse}

trait BaseApi {

  /**
   * sets the expire time (in sec.) for the specified key.
   */
  def expire(key: Any, ttl: Int)(implicit format: Format): Boolean

  /**
   * removes data from all the DB's.
   */
  def flushall: Boolean

  /**
   * exits the server.
   */
  def quit: Boolean

  /**
   * ping
   */
  def ping: Option[String]

  protected val pong: Option[String] = Some("PONG")
}
