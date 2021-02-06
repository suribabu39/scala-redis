package com

import com.redis.RedisClient

object Main extends App {

  val r = new RedisClient()
  println(r.set("key", "some value"))
  println(r.get("key").getOrElse("Key not found"))
  r.close()
}
