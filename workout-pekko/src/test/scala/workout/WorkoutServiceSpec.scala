package workout

import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.time.{Millis, Seconds, Span}
import workout.config.DatabaseConfig
import workout.domain._
import workout.kafka.NoopEventProducer
import workout.repository.{DatabaseProvider, FlywayMigration, SlickWorkoutRepository}
import workout.service.WorkoutServiceImpl

import scala.concurrent.ExecutionContext.Implicits.global
import java.util.UUID

class WorkoutServiceSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with ForAllTestContainer
    with BeforeAndAfterAll {

  implicit val patience: PatienceConfig = PatienceConfig(
    timeout = Span(10, Seconds),
    interval = Span(100, Millis)
  )

  override val container: PostgreSQLContainer = PostgreSQLContainer()

  lazy val dbConfig: DatabaseConfig = DatabaseConfig(
    driver = "org.postgresql.Driver",
    url = container.jdbcUrl,
    user = container.username,
    password = container.password,
    numThreads = 5
  )

  lazy val database = DatabaseProvider.createDatabase(dbConfig)
  lazy val repository = new SlickWorkoutRepository(database)
  lazy val eventProducer = new NoopEventProducer()
  lazy val service = new WorkoutServiceImpl(repository, eventProducer)

  override def beforeAll(): Unit = {
    super.beforeAll()
    container.start()
    FlywayMigration.migrate(dbConfig)
  }

  override def afterAll(): Unit = {
    database.close()
    container.stop()
    super.afterAll()
  }

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

  "WorkoutService" should {

    "create a workout" in {
      val result = service.create(sampleCreateRequest).futureValue

      result.name shouldBe sampleCreateRequest.name
      result.description shouldBe sampleCreateRequest.description
      result.workoutType shouldBe sampleCreateRequest.workoutType
      result.durationMinutes shouldBe sampleCreateRequest.durationMinutes
      result.difficulty shouldBe sampleCreateRequest.difficulty
    }

    "find workout by ID" in {
      val created = service.create(sampleCreateRequest).futureValue
      val found = service.findById(created.id).futureValue

      found shouldBe defined
      found.get.id shouldBe created.id
      found.get.name shouldBe created.name
    }

    "return None for non-existent workout" in {
      val found = service.findById(UUID.randomUUID()).futureValue

      found shouldBe None
    }

    "list all workouts" in {
      service.create(sampleCreateRequest).futureValue
      service.create(sampleCreateRequest2).futureValue

      val workouts = service.findAll(100, 0).futureValue

      workouts.size should be >= 2
    }

    "update a workout" in {
      val created = service.create(sampleCreateRequest).futureValue

      val updateReq = UpdateWorkoutRequest(
        name = Some("Updated Run"),
        description = None,
        workoutType = None,
        durationMinutes = Some(45),
        caloriesBurned = None,
        difficulty = Some(Difficulty.Intermediate)
      )

      val updated = service.update(created.id, updateReq).futureValue

      updated shouldBe defined
      updated.get.name shouldBe "Updated Run"
      updated.get.durationMinutes shouldBe 45
      updated.get.difficulty shouldBe Difficulty.Intermediate
      // Original values should be preserved
      updated.get.workoutType shouldBe sampleCreateRequest.workoutType
    }

    "delete a workout" in {
      val created = service.create(sampleCreateRequest).futureValue

      val deleted = service.delete(created.id).futureValue
      val found = service.findById(created.id).futureValue

      deleted shouldBe true
      found shouldBe None
    }

    "find workouts by type" in {
      service.create(sampleCreateRequest).futureValue  // Running
      service.create(sampleCreateRequest2).futureValue // HIIT

      val running = service.findByType(WorkoutType.Running).futureValue
      val hiit = service.findByType(WorkoutType.HIIT).futureValue

      running.forall(_.workoutType == WorkoutType.Running) shouldBe true
      hiit.forall(_.workoutType == WorkoutType.HIIT) shouldBe true
    }

    "find workouts by difficulty" in {
      service.create(sampleCreateRequest).futureValue  // Beginner
      service.create(sampleCreateRequest2).futureValue // Advanced

      val beginners = service.findByDifficulty(Difficulty.Beginner).futureValue
      val advanced = service.findByDifficulty(Difficulty.Advanced).futureValue

      beginners.forall(_.difficulty == Difficulty.Beginner) shouldBe true
      advanced.forall(_.difficulty == Difficulty.Advanced) shouldBe true
    }
  }

  "WorkoutType" should {

    "parse valid types" in {
      WorkoutType.fromString("Running") shouldBe Right(WorkoutType.Running)
      WorkoutType.fromString("HIIT") shouldBe Right(WorkoutType.HIIT)
      WorkoutType.fromString("running") shouldBe Right(WorkoutType.Running)
    }

    "reject invalid types" in {
      WorkoutType.fromString("Invalid").isLeft shouldBe true
    }
  }

  "Difficulty" should {

    "parse valid values" in {
      Difficulty.fromString("Beginner") shouldBe Right(Difficulty.Beginner)
      Difficulty.fromString("Advanced") shouldBe Right(Difficulty.Advanced)
      Difficulty.fromString("beginner") shouldBe Right(Difficulty.Beginner)
    }

    "reject invalid values" in {
      Difficulty.fromString("Invalid").isLeft shouldBe true
    }
  }
}

