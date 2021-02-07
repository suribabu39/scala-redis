package utils

import java.io.File

object FileUtils {

  def getListOfFiles(dir: String): List[String] = {
    val file = new File(dir)
    if (file.listFiles == null)
      throw new Exception(s"FilesNotFound in directory - $dir")
    file.listFiles
      .filter(_.isFile)
      .map(_.getPath)
      .filter(_.endsWith(".txt"))
      .toList
  }
}
