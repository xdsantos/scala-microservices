package workout.domain

import io.circe.{Decoder, Encoder}

sealed trait Difficulty {
  def value: String
}

object Difficulty {
  case object Beginner extends Difficulty { val value = "Beginner" }
  case object Intermediate extends Difficulty { val value = "Intermediate" }
  case object Advanced extends Difficulty { val value = "Advanced" }
  case object Expert extends Difficulty { val value = "Expert" }

  val values: Seq[Difficulty] = Seq(Beginner, Intermediate, Advanced, Expert)

  def fromString(s: String): Either[String, Difficulty] =
    values.find(_.value.equalsIgnoreCase(s))
      .toRight(s"Invalid difficulty: $s. Valid values: ${values.map(_.value).mkString(", ")}")

  implicit val encoder: Encoder[Difficulty] = Encoder.encodeString.contramap(_.value)
  implicit val decoder: Decoder[Difficulty] = Decoder.decodeString.emap(fromString)
}

