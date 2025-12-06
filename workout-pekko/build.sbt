ThisBuild / scalaVersion := "2.13.14"
ThisBuild / organization := "com.workout"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    name := "workout-pekko-service",

    resolvers += "Apache Pekko Snapshots" at "https://repository.apache.org/content/groups/snapshots/",

    libraryDependencies ++= {
      val pekkoVersion = "1.0.2"
      val pekkoHttpVersion = "1.0.0"
      val pekkoConnectorsVersion = "1.0.0"
      val slickVersion = "3.4.1"
      val flywayVersion = "9.22.3"
      val postgresVersion = "42.6.0"
      val circeVersion = "0.14.6"
      val testcontainersVersion = "0.41.0"

      Seq(
        // Pekko
        "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
        "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
        "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
        "org.apache.pekko" %% "pekko-http-spray-json" % pekkoHttpVersion,

        // Pekko Connectors (Alpakka) Kafka
        "org.apache.pekko" %% "pekko-connectors-kafka" % pekkoConnectorsVersion,

        // JSON with Circe
        "io.circe" %% "circe-core" % circeVersion,
        "io.circe" %% "circe-generic" % circeVersion,
        "io.circe" %% "circe-parser" % circeVersion,

        // Slick for database
        "com.typesafe.slick" %% "slick" % slickVersion,
        "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
        "org.postgresql" % "postgresql" % postgresVersion,

        // Flyway migrations
        "org.flywaydb" % "flyway-core" % flywayVersion,

        // Configuration
        "com.typesafe" % "config" % "1.4.3",

        // Logging
        "ch.qos.logback" % "logback-classic" % "1.4.11",
        "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",

        // Testing
        "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion % Test,
        "org.apache.pekko" %% "pekko-http-testkit" % pekkoHttpVersion % Test,
        "org.apache.pekko" %% "pekko-stream-testkit" % pekkoVersion % Test,
        "org.scalatest" %% "scalatest" % "3.2.17" % Test,
        "com.dimafeng" %% "testcontainers-scala-scalatest" % testcontainersVersion % Test,
        "com.dimafeng" %% "testcontainers-scala-postgresql" % testcontainersVersion % Test,
        "com.dimafeng" %% "testcontainers-scala-kafka" % testcontainersVersion % Test
      )
    },

    // Docker settings
    Docker / packageName := "workout-pekko-service",
    Docker / version := version.value,
    dockerBaseImage := "eclipse-temurin:17-jre-alpine",
    dockerExposedPorts := Seq(8089),
    dockerUpdateLatest := true,

    // Native packager settings
    Compile / mainClass := Some("workout.Main"),

    // Scala compiler options
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked"
    )
  )

