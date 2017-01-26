name := "twitter-statistics"

version := "2.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.0.1",
  "io.circe" %% "circe-core" % "0.5.0",
  "io.circe" %% "circe-generic" % "0.5.0",
  "io.circe" %% "circe-parser" % "0.5.0",
  "com.hunorkovacs" %% "koauth-sync" % "1.1.0",
  "org.apache.logging.log4j" % "log4j-api" % "2.7",
  "org.apache.logging.log4j" % "log4j-core" % "2.7",
  "com.lmax" % "disruptor" % "3.3.6"
)
