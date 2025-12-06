package workout

import com.dimafeng.testcontainers.PostgreSQLContainer
import workout.config._
import workout.domain._
import workout.kafka.WorkoutEventProducerNoop
import workout.repository._
import workout.service._
import workout.telemetry.TracingNoop
import zio._
import zio.test._
import zio.test.Assertion._
import java.util.UUID
import javax.sql.DataSource

object WorkoutServiceSpec extends ZIOSpecDefault {

  // Test container setup
  val postgresContainer: ZLayer[Any, Throwable, PostgreSQLContainer] =
    ZLayer.scoped {
      ZIO.acquireRelease(
        ZIO.attemptBlocking {
          val container = PostgreSQLContainer()
          container.start()
          container
        }
      )(container => ZIO.attemptBlocking(container.stop()).orDie)
    }

  // Create database config from container
  val testDatabaseConfig: ZLayer[PostgreSQLContainer, Nothing, DatabaseConfig] =
    ZLayer.fromZIO {
      ZIO.serviceWith[PostgreSQLContainer] { container =>
        DatabaseConfig(
          driver = "org.postgresql.Driver",
          url = container.jdbcUrl,
          user = container.username,
          password = container.password,
          poolSize = 5
        )
      }
    }

  // Test DataSource layer
  val testDataSource: ZLayer[DatabaseConfig, Throwable, DataSource] =
    DataSourceLive.layer

  // Full test layer with WorkoutService
  val testLayer: ZLayer[Any, Throwable, WorkoutService with DatabaseConfig] =
    postgresContainer >>>
      testDatabaseConfig >>>
      (ZLayer.service[DatabaseConfig] ++ testDataSource) >>>
      (ZLayer.service[DatabaseConfig] ++ QuillLive.layer) >>>
      (ZLayer.service[DatabaseConfig] ++ WorkoutRepositoryLive.layer) >>>
      (ZLayer.service[DatabaseConfig] ++ WorkoutEventProducerNoop.layer ++ TracingNoop.layer ++ ZLayer.service[WorkoutRepository]) >>>
      (ZLayer.service[DatabaseConfig] ++ WorkoutServiceLive.layer)

  // Helper to run migrations before tests
  def withMigrations[R <: DatabaseConfig, E, A](test: ZIO[R, E, A]): ZIO[R, Any, A] =
    for {
      config <- ZIO.service[DatabaseConfig]
      _ <- FlywayMigration.migrate.provide(
        FlywayMigrationLive.layer,
        ZLayer.succeed(config)
      )
      result <- test
    } yield result

  // Sample workout request
  val sampleCreateRequest: CreateWorkoutRequest = CreateWorkoutRequest(
    name = "Morning Run",
    description = Some("Easy 5K morning run"),
    workoutType = WorkoutType.Running,
    durationMinutes = 30,
    caloriesBurned = Some(300),
    difficulty = Difficulty.Beginner
  )

  val sampleCreateRequest2: CreateWorkoutRequest = CreateWorkoutRequest(
    name = "HIIT Session",
    description = Some("High intensity interval training"),
    workoutType = WorkoutType.HIIT,
    durationMinutes = 45,
    caloriesBurned = Some(500),
    difficulty = Difficulty.Advanced
  )

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("WorkoutService")(
    suite("CRUD Operations")(
      test("should create a workout") {
        withMigrations {
          for {
            workout <- WorkoutService.create(sampleCreateRequest)
          } yield assertTrue(
            workout.name == sampleCreateRequest.name,
            workout.description == sampleCreateRequest.description,
            workout.workoutType == sampleCreateRequest.workoutType,
            workout.durationMinutes == sampleCreateRequest.durationMinutes,
            workout.difficulty == sampleCreateRequest.difficulty
          )
        }
      },

      test("should find workout by ID") {
        withMigrations {
          for {
            created <- WorkoutService.create(sampleCreateRequest)
            found <- WorkoutService.findById(created.id)
          } yield assertTrue(
            found.isDefined,
            found.get.id == created.id,
            found.get.name == created.name
          )
        }
      },

      test("should return None for non-existent workout") {
        withMigrations {
          for {
            found <- WorkoutService.findById(UUID.randomUUID())
          } yield assertTrue(found.isEmpty)
        }
      },

      test("should list all workouts") {
        withMigrations {
          for {
            _ <- WorkoutService.create(sampleCreateRequest)
            _ <- WorkoutService.create(sampleCreateRequest2)
            workouts <- WorkoutService.findAll(100, 0)
          } yield assertTrue(workouts.size >= 2)
        }
      },

      test("should update a workout") {
        withMigrations {
          for {
            created <- WorkoutService.create(sampleCreateRequest)
            updateReq = UpdateWorkoutRequest(
              name = Some("Updated Run"),
              description = None,
              workoutType = None,
              durationMinutes = Some(45),
              caloriesBurned = None,
              difficulty = Some(Difficulty.Intermediate)
            )
            updated <- WorkoutService.update(created.id, updateReq)
          } yield assertTrue(
            updated.isDefined,
            updated.get.name == "Updated Run",
            updated.get.durationMinutes == 45,
            updated.get.difficulty == Difficulty.Intermediate,
            // Original values should be preserved
            updated.get.workoutType == sampleCreateRequest.workoutType
          )
        }
      },

      test("should delete a workout") {
        withMigrations {
          for {
            created <- WorkoutService.create(sampleCreateRequest)
            deleted <- WorkoutService.delete(created.id)
            found <- WorkoutService.findById(created.id)
          } yield assertTrue(
            deleted,
            found.isEmpty
          )
        }
      }
    ),

    suite("Filter Operations")(
      test("should find workouts by type") {
        withMigrations {
          for {
            _ <- WorkoutService.create(sampleCreateRequest)  // Running
            _ <- WorkoutService.create(sampleCreateRequest2) // HIIT
            running <- WorkoutService.findByType(WorkoutType.Running)
            hiit <- WorkoutService.findByType(WorkoutType.HIIT)
          } yield assertTrue(
            running.forall(_.workoutType == WorkoutType.Running),
            hiit.forall(_.workoutType == WorkoutType.HIIT)
          )
        }
      },

      test("should find workouts by difficulty") {
        withMigrations {
          for {
            _ <- WorkoutService.create(sampleCreateRequest)  // Beginner
            _ <- WorkoutService.create(sampleCreateRequest2) // Advanced
            beginners <- WorkoutService.findByDifficulty(Difficulty.Beginner)
            advanced <- WorkoutService.findByDifficulty(Difficulty.Advanced)
          } yield assertTrue(
            beginners.forall(_.difficulty == Difficulty.Beginner),
            advanced.forall(_.difficulty == Difficulty.Advanced)
          )
        }
      }
    ),

    suite("Domain Models")(
      test("WorkoutType should parse valid types") {
        import zio.json._
        val json = "\"Running\""
        val result = json.fromJson[WorkoutType]
        assertTrue(result == Right(WorkoutType.Running))
      },

      test("WorkoutType should reject invalid types") {
        import zio.json._
        val json = "\"Invalid\""
        val result = json.fromJson[WorkoutType]
        assertTrue(result.isLeft)
      },

      test("Difficulty should parse valid values") {
        import zio.json._
        val json = "\"Advanced\""
        val result = json.fromJson[Difficulty]
        assertTrue(result == Right(Difficulty.Advanced))
      },

      test("CreateWorkoutRequest should serialize/deserialize") {
        import zio.json._
        val json = sampleCreateRequest.toJson
        val parsed = json.fromJson[CreateWorkoutRequest]
        assertTrue(parsed == Right(sampleCreateRequest))
      }
    )
  ).provideLayerShared(testLayer) @@ TestAspect.sequential
}
