package workoutcats.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import java.time.Instant
import java.util.UUID

final case class Workout(
    id: UUID,
    name: String,
    description: Option[String],
    workoutType: WorkoutType,
    durationMinutes: Int,
    caloriesBurned: Option[Int],
    difficulty: Difficulty,
    createdAt: Instant,
    updatedAt: Instant
)

object Workout {
  implicit val enc: Encoder[Workout] = deriveEncoder
  implicit val dec: Decoder[Workout] = deriveDecoder
}

