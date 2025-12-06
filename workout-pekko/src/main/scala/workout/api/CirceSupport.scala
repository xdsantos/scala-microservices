package workout.api

import org.apache.pekko.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, MediaTypes}
import org.apache.pekko.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import io.circe.{Decoder, Encoder, Printer, parser}

import scala.concurrent.Future

trait CirceSupport {
  private val printer = Printer.noSpaces.copy(dropNullValues = true)

  implicit def circeUnmarshaller[A: Decoder]: FromEntityUnmarshaller[A] =
    Unmarshaller.stringUnmarshaller
      .forContentTypes(ContentTypes.`application/json`, MediaTypes.`application/json`)
      .flatMap { _ => _ => json =>
        parser.decode[A](json) match {
          case Right(value) => Future.successful(value)
          case Left(error)  => Future.failed(new RuntimeException(error.getMessage))
        }
      }

  implicit def circeMarshaller[A: Encoder]: ToEntityMarshaller[A] =
    Marshaller.withFixedContentType(ContentTypes.`application/json`) { value =>
      HttpEntity(ContentTypes.`application/json`, printer.print(Encoder[A].apply(value)))
    }
}

object CirceSupport extends CirceSupport

