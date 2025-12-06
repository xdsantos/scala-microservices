package workout.domain

import io.circe.{Decoder, Encoder}

sealed trait WorkoutType {
  def value: String
}

object WorkoutType {
  case object Cardio extends WorkoutType { val value = "Cardio" }
  case object Strength extends WorkoutType { val value = "Strength" }
  case object Flexibility extends WorkoutType { val value = "Flexibility" }
  case object HIIT extends WorkoutType { val value = "HIIT" }
  case object Yoga extends WorkoutType { val value = "Yoga" }
  case object CrossFit extends WorkoutType { val value = "CrossFit" }
  case object Swimming extends WorkoutType { val value = "Swimming" }
  case object Running extends WorkoutType { val value = "Running" }
  case object Cycling extends WorkoutType { val value = "Cycling" }
  case object Other extends WorkoutType { val value = "Other" }

  val values: Seq[WorkoutType] = Seq(
    Cardio, Strength, Flexibility, HIIT, Yoga, CrossFit, Swimming, Running, Cycling, Other
  )

  def fromString(s: String): Either[String, WorkoutType] =
    values.find(_.value.equalsIgnoreCase(s))
      .toRight(s"Invalid workout type: $s. Valid types: ${values.map(_.value).mkString(", ")}")

  implicit val encoder: Encoder[WorkoutType] = Encoder.encodeString.contramap(_.value)
  implicit val decoder: Decoder[WorkoutType] = Decoder.decodeString.emap(fromString)
}

