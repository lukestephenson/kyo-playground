
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.5.0"

val kyoVersion = "0.11.1"
lazy val root = (project in file("."))
  .settings(
    name := "kyo-playground"
  )

libraryDependencies += "io.monix" %% "monix" % "3.4.1"

libraryDependencies += "io.getkyo" %% "kyo-core" % kyoVersion
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.5.6"
//libraryDependencies += "io.getkyo" %% "kyo-direct" % kyoVersion
//libraryDependencies += "io.getkyo" %% "kyo-cache" % kyoVersion
//libraryDependencies += "io.getkyo" %% "kyo-stats-otel" % kyoVersion
//libraryDependencies += "io.getkyo" %% "kyo-sttp" % kyoVersion
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "3.8.0"
libraryDependencies += "dev.zio" %% "zio-kafka"         % "2.8.2"