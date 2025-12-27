package workoutcats.domain

import io.circe.{Decoder, Encoder}

sealed trait Difficulty
object Difficulty {
  case object Beginner extends Difficulty
  case object Intermediate extends Difficulty
  case object Advanced extends Difficulty
  case object Expert extends Difficulty

  val values: List[Difficulty] = List(Beginner, Intermediate, Advanced, Expert)

  def fromString(s: String): Either[String, Difficulty] =
    values.find(_.toString.equalsIgnoreCase(s)).toRight(s"Invalid difficulty: $s")

  implicit val encoder: Encoder[Difficulty] = Encoder.encodeString.contramap(_.toString)
  implicit val decoder: Decoder[Difficulty] = Decoder.decodeString.emap(fromString)
}

