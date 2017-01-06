name := "twitter-statistics"

version := "2.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.0.1",
  "io.circe" %% "circe-core" % "0.5.0",
  "io.circe" %% "circe-generic" % "0.5.0",
  "io.circe" %% "circe-parser" % "0.5.0",
  "com.hunorkovacs" %% "koauth" % "1.1.0"
)
