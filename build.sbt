name := "streamsTutorial"

version := "1.0"

scalaVersion := "2.12.2"

lazy val akkaVersion = "2.5.3"

lazy val playJsonVersion = "2.6.0"

resolvers ++= Seq(
  "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
  "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.play" %% "play-json" % playJsonVersion
)
