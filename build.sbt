
name := "streamsTutorial"

version := "1.0"

scalaVersion := "2.12.2"

lazy val akkaVersion = "2.5.3"

lazy val playJsonVersion = "2.6.0"

lazy val playWsVersion = "1.0.0"

resolvers ++= Seq(
  "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
  "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.play" %% "play-json" % playJsonVersion,
  "com.typesafe.play" % "play-ws-standalone_2.12" % playWsVersion,
  "com.typesafe.play" % "play-ahc-ws-standalone_2.12" % playWsVersion,
  "com.typesafe.play" % "play-ws-standalone-json_2.12" % playWsVersion,
  "com.typesafe.play" %% "play-ws-standalone-xml" % "1.0.1",
  "com.sksamuel.elastic4s" %% "elastic4s-core" % "5.4.10",
  "com.sksamuel.elastic4s" %% "elastic4s-http" % "5.4.10"
)
