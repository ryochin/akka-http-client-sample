import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.4",
      version      := "0.0.1"
    )),
    name := "Akka-HTTP Client Sample Project",
    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlint"),
    scalacOptions ++= Seq("-Ywarn-dead-code",
                          "-Ywarn-numeric-widen",
                          "-Ywarn-unused",
                          "-Ywarn-unused-import",
                          "-Ywarn-value-discard",
                          "-encoding", "utf8"),
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
      "com.typesafe.akka" %% "akka-http" % "10.0.11",
      "com.typesafe.akka" %% "akka-stream" % "2.5.8",
      "com.typesafe.akka" %% "akka-actor" % "2.5.8"
    )
  )
