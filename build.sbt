
name := "remote-executor"

version := "1.0"

scalaVersion := "2.11.4"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies +=
  "com.typesafe.akka" %% "akka-actor" % "2.3.7"

libraryDependencies +=
  "com.typesafe.akka" %% "akka-remote" % "2.3.7"

libraryDependencies +=  "org.scalaj" %% "scalaj-http" % "2.3.0"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0"
