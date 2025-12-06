package workout.api

import io.circe.{Decoder, DecodingFailure, Encoder, Json, ParsingFailure, Printer}
import io.circe.parser
import org.apache.pekko.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, MediaTypes}
import org.apache.pekko.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}

import scala.concurrent.Future

trait CirceSupport {

  // Switch between compact and pretty JSON output if desired
  protected val printer: Printer = Printer.spaces2.copy(dropNullValues = true)

  // ---- UNMARSHALLER (JSON -> Scala) ----
  implicit def circeUnmarshaller[A: Decoder]: FromEntityUnmarshaller[A] =
    Unmarshaller.stringUnmarshaller
      .forContentTypes(
        ContentTypes.`application/json`,
        MediaTypes.`application/json`
      )
      .flatMap { _ => _ => json =>
        parser.decode[A](json) match {
          case Right(value) => Future.successful(value)

          case Left(e: DecodingFailure) =>
            Future.failed(new IllegalArgumentException(
              s"Invalid JSON structure: ${e.getMessage}"
            ))

          case Left(e: ParsingFailure) =>
            Future.failed(new IllegalArgumentException(
              s"Malformed JSON: ${e.message}"
            ))

          case Left(other) =>
            Future.failed(new IllegalArgumentException(other.getMessage))
        }
      }

  // ---- MARSHALLER (Scala -> JSON) ----
  implicit def circeMarshaller[A: Encoder]: ToEntityMarshaller[A] =
    Marshaller.withFixedContentType(ContentTypes.`application/json`) { value =>
      val json: Json = Encoder[A].apply(value)
      HttpEntity(ContentTypes.`application/json`, printer.print(json))
    }
}

object CirceSupport extends CirceSupport

