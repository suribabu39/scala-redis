package com.redis.api

import com.redis.serialization.{Format, Parse}

trait BaseApi {

  /**
   * sets the expire time (in sec.) for the specified key.
   */
  def expire(key: Any, ttl: Int)(implicit format: Format): Boolean

  /**
   * selects the DB to connect, defaults to 0 (zero).
   */
  def select(index: Int): Boolean

  /**
   * removes all the DB data.
   */
  def flushdb: Boolean

  /**
   * removes data from all the DB's.
   */
  def flushall: Boolean

  /**
   * exits the server.
   */
  def quit: Boolean

  /**
   * auths with the server.
   */
  def auth(secret: Any)(implicit format: Format): Boolean

  /**
   * ping
   */
  def ping: Option[String]

  protected val pong: Option[String] = Some("PONG")
}
