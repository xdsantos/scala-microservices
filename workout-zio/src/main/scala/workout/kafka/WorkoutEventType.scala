package workout.kafka

import zio.json._

sealed trait WorkoutEventType

object WorkoutEventType {
  case object Created extends WorkoutEventType
  case object Updated extends WorkoutEventType
  case object Deleted extends WorkoutEventType

  val values: Seq[WorkoutEventType] = Seq(Created, Updated, Deleted)

  implicit val encoder: JsonEncoder[WorkoutEventType] = JsonEncoder[String].contramap(_.toString)
  implicit val decoder: JsonDecoder[WorkoutEventType] = JsonDecoder[String].mapOrFail { s =>
    values.find(_.toString.equalsIgnoreCase(s))
      .toRight(s"Invalid event type: $s")
  }
}
