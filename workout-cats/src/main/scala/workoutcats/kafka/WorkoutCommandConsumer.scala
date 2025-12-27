package workoutcats.kafka

import cats.effect.kernel.MonadCancelThrow
import cats.effect.kernel.Resource
import cats.syntax.all._
import fs2.kafka._
import workoutcats.config.KafkaConfig
import workoutcats.service.WorkoutService
import workoutcats.telemetry.Tracing
import io.circe.parser.decode
import io.opentelemetry.api.trace.SpanKind

final class WorkoutCommandConsumer[F[_]: MonadCancelThrow](
    consumer: KafkaConsumer[F, String, String],
    service: WorkoutService[F],
    tracing: Tracing[F],
    config: KafkaConfig
) {
  def stream: fs2.Stream[F, Unit] =
    fs2.Stream.eval(consumer.subscribeTo(config.commandTopic)) >>
      consumer.stream.evalMap { committable =>
        val value = committable.record.value
        val process: F[Unit] =
          decode[CreateWorkoutCommand](value) match {
            case Right(cmd) =>
              val withSpan =
                (cmd.traceId, cmd.spanId) match {
                  case (Some(tid), Some(sid)) if tid.nonEmpty && sid.nonEmpty =>
                    tracing.spanWithParent("KafkaConsumer.processMessage", tid, sid, SpanKind.CONSUMER) {
                      handleCommand(cmd)
                    }
                  case _ =>
                    tracing.span("KafkaConsumer.processMessage", SpanKind.CONSUMER) {
                      handleCommand(cmd)
                    }
                }
              withSpan
            case Left(err) =>
              tracing.logWarn(s"Failed to decode command: $err")
          }
        process *> committable.offset.commit
      }

  private def handleCommand(cmd: CreateWorkoutCommand): F[Unit] =
    for {
      _ <- tracing.addAttribute("messaging.system", "kafka")
      _ <- tracing.addAttribute("messaging.destination", config.commandTopic)
      _ <- tracing.addAttribute("messaging.operation", "process")
      _ <- tracing.addAttribute("correlation.id", cmd.correlationId.toString)
      _ <- tracing.addAttribute("workout.name", cmd.request.name)
      w <- service.create(cmd.request)
      _ <- tracing.addAttribute("workout.id", w.id.toString)
    } yield ()
}

