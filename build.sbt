
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.2"

val kyoVersion = "0.16.2"
val logbackVersion = "1.5.17"

lazy val root = (project in file("."))
  .settings(
    name := "kyo-playground"
  )

libraryDependencies += "io.monix" %% "monix" % "3.4.1"

libraryDependencies += "io.getkyo" %% "kyo-core" % kyoVersion
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.5.11"
//libraryDependencies += "io.getkyo" %% "kyo-direct" % kyoVersion
//libraryDependencies += "io.getkyo" %% "kyo-cache" % kyoVersion
//libraryDependencies += "io.getkyo" %% "kyo-stats-otel" % kyoVersion
//libraryDependencies += "io.getkyo" %% "kyo-sttp" % kyoVersion
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "3.8.0"
libraryDependencies += "dev.zio" %% "zio-kafka"         % "2.8.2"
libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-core" % logbackVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
)