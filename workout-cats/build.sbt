ThisBuild / scalaVersion := "2.13.14"
ThisBuild / organization := "com.workout"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "workout-cats-service",
    libraryDependencies ++= {
      val catsEffectVersion = "3.5.4"
      val http4sVersion = "0.23.24"
      val circeVersion = "0.14.6"
      val doobieVersion = "1.0.0-RC4"
      val fs2KafkaVersion = "3.5.1"
      val flywayVersion = "9.22.3"
      val postgresVersion = "42.6.0"
      val otelVersion = "1.32.0"

      Seq(
        "org.typelevel" %% "cats-effect" % catsEffectVersion,
        "org.http4s" %% "http4s-ember-server" % http4sVersion,
        "org.http4s" %% "http4s-circe" % http4sVersion,
        "org.http4s" %% "http4s-dsl" % http4sVersion,
        "io.circe" %% "circe-core" % circeVersion,
        "io.circe" %% "circe-generic" % circeVersion,
        "io.circe" %% "circe-parser" % circeVersion,

        // DB
        "org.tpolecat" %% "doobie-core" % doobieVersion,
        "org.tpolecat" %% "doobie-hikari" % doobieVersion,
        "org.tpolecat" %% "doobie-postgres" % doobieVersion,
        "org.flywaydb" % "flyway-core" % flywayVersion,
        "org.postgresql" % "postgresql" % postgresVersion,

        // Kafka
        "com.github.fd4s" %% "fs2-kafka" % fs2KafkaVersion,

        // OpenTelemetry (manual wiring)
        "io.opentelemetry" % "opentelemetry-api" % otelVersion,
        "io.opentelemetry" % "opentelemetry-sdk" % otelVersion,
        "io.opentelemetry" % "opentelemetry-exporter-otlp" % otelVersion,

        // Config / logging
        "com.typesafe" % "config" % "1.4.3",
        "ch.qos.logback" % "logback-classic" % "1.4.11"
      )
    },
    Compile / mainClass := Some("workoutcats.Main"),
    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")
  )

