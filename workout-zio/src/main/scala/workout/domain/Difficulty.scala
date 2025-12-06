package workout.domain

import zio.json._

sealed trait Difficulty

object Difficulty {
  case object Beginner extends Difficulty
  case object Intermediate extends Difficulty
  case object Advanced extends Difficulty
  case object Expert extends Difficulty

  val values: Seq[Difficulty] = Seq(Beginner, Intermediate, Advanced, Expert)

  def fromString(s: String): Either[String, Difficulty] =
    values.find(_.toString.equalsIgnoreCase(s))
      .toRight(s"Invalid difficulty: $s. Valid values: ${values.map(_.toString).mkString(", ")}")

  implicit val encoder: JsonEncoder[Difficulty] = JsonEncoder[String].contramap(_.toString)
  implicit val decoder: JsonDecoder[Difficulty] = JsonDecoder[String].mapOrFail { s =>
    fromString(s)
  }
}
