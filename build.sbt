
name := "RedisClient"
version := "0.1"
scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "org.apache.commons"      %  "commons-pool2"           % "2.8.0",
  "org.scalatest"           %% "scalatest"               % "3.1.0"       % "test",
  "com.typesafe"            % "config"                   % "1.4.0"
)

assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".class" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith "logback.xml" => MergeStrategy.concat
  case "application.conf"                            => MergeStrategy.concat
  case "unwanted.txt"                                => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

initialCommands in console := "import java.net.URLEncoder"
