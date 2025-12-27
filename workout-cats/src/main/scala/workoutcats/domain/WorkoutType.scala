package workoutcats.domain

import io.circe.{Decoder, Encoder}

sealed trait WorkoutType
object WorkoutType {
  case object Cardio extends WorkoutType
  case object Strength extends WorkoutType
  case object Flexibility extends WorkoutType
  case object HIIT extends WorkoutType
  case object Yoga extends WorkoutType
  case object CrossFit extends WorkoutType
  case object Swimming extends WorkoutType
  case object Running extends WorkoutType
  case object Cycling extends WorkoutType
  case object Other extends WorkoutType

  val values: List[WorkoutType] =
    List(Cardio, Strength, Flexibility, HIIT, Yoga, CrossFit, Swimming, Running, Cycling, Other)

  def fromString(s: String): Either[String, WorkoutType] =
    values.find(_.toString.equalsIgnoreCase(s)).toRight(s"Invalid workout type: $s")

  implicit val encoder: Encoder[WorkoutType] = Encoder.encodeString.contramap(_.toString)
  implicit val decoder: Decoder[WorkoutType] = Decoder.decodeString.emap(fromString)
}

