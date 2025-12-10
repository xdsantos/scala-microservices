ThisBuild / scalaVersion := "2.13.14"
ThisBuild / organization := "com.workout"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    name := "workout-service",
    
    // ZIO versions
    libraryDependencies ++= {
      val zioVersion = "2.0.19"
      val zioHttpVersion = "3.0.0-RC4"
      val zioKafkaVersion = "2.7.0"
      val zioConfigVersion = "4.0.0-RC16"
      val zioJsonVersion = "0.6.2"
      val quillVersion = "4.8.0"
      val flywayVersion = "9.22.3"
      val postgresVersion = "42.6.0"
      val otelVersion = "1.32.0"
      val testcontainersVersion = "0.41.0"

      Seq(
        // ZIO Core
        "dev.zio" %% "zio" % zioVersion,
        "dev.zio" %% "zio-streams" % zioVersion,
        
        // ZIO HTTP
        "dev.zio" %% "zio-http" % zioHttpVersion,
        
        // ZIO Kafka
        "dev.zio" %% "zio-kafka" % zioKafkaVersion,
        
        // ZIO Config
        "dev.zio" %% "zio-config" % zioConfigVersion,
        "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
        "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
        
        // ZIO JSON
        "dev.zio" %% "zio-json" % zioJsonVersion,
        
        // Quill for database
        "io.getquill" %% "quill-jdbc-zio" % quillVersion,
        "org.postgresql" % "postgresql" % postgresVersion,
        
        // Flyway migrations
        "org.flywaydb" % "flyway-core" % flywayVersion,
        
        // Configuration
        "com.typesafe" % "config" % "1.4.3",
        
        // OpenTelemetry (manual wiring to avoid auto-global registration)
        "io.opentelemetry" % "opentelemetry-api" % otelVersion,
        "io.opentelemetry" % "opentelemetry-sdk" % otelVersion,
        "io.opentelemetry" % "opentelemetry-exporter-otlp" % otelVersion,
        
        // Logging
        "ch.qos.logback" % "logback-classic" % "1.4.11",
        "dev.zio" %% "zio-logging" % "2.1.16",
        "dev.zio" %% "zio-logging-slf4j" % "2.1.16",
        
        // Testing
        "dev.zio" %% "zio-test" % zioVersion % Test,
        "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
        "dev.zio" %% "zio-test-magnolia" % zioVersion % Test,
        "com.dimafeng" %% "testcontainers-scala-postgresql" % testcontainersVersion % Test,
        "com.dimafeng" %% "testcontainers-scala-kafka" % testcontainersVersion % Test,
        "com.dimafeng" %% "testcontainers-scala-core" % testcontainersVersion % Test
      )
    },
    
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    
    // Docker settings
    Docker / packageName := "workout-service",
    Docker / version := version.value,
    dockerBaseImage := "eclipse-temurin:17-jre-alpine",
    dockerExposedPorts := Seq(8080),
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

