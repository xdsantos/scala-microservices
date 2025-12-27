package workoutcats.kafka

import io.circe.{Decoder, Encoder}

sealed trait WorkoutEventType { def value: String }
object WorkoutEventType {
  case object Created extends WorkoutEventType { val value = "Created" }
  case object Updated extends WorkoutEventType { val value = "Updated" }
  case object Deleted extends WorkoutEventType { val value = "Deleted" }

  implicit val enc: Encoder[WorkoutEventType] = Encoder.encodeString.contramap(_.value)
  implicit val dec: Decoder[WorkoutEventType] = Decoder.decodeString.emap {
    case "Created" => Right(Created)
    case "Updated" => Right(Updated)
    case "Deleted" => Right(Deleted)
    case other     => Left(s"Invalid event type: $other")
  }
}

