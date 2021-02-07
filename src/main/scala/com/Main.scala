package com

import java.util.concurrent.{Executors, TimeUnit}
import utils.FileUtils

object Main {

  def main(args: Array[String]): Unit = {

    val inputDir = System.getProperty("inputDir", "files/")
    val poolSize = System.getProperty("users", "2").toInt
    val files = FileUtils.getListOfFiles(inputDir)
    println(s"Num of users/files - $files")

    val executorService = Executors.newFixedThreadPool(poolSize)

    try {
      println("Parallely executing all the users ...\n")
      files.zipWithIndex.foreach {
        case (file, idx) =>
          executorService.execute(new User(idx+1, file))
      }

      executorService.shutdown()
      executorService.awaitTermination(10, TimeUnit.SECONDS)
      println("Application exiting successfully")
    } catch {
      case ex: Throwable => {
        println("Exception occurred while executing user queries")
        ex.printStackTrace
        println("Application exiting with failure status")
      }
    }
  }
}
