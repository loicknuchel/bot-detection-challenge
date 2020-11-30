package fr.loicknuchel.botless.server.api.domain

import cats.effect.Sync
import fr.loicknuchel.botless.shared.domain.AnalyzedLog
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

import scala.concurrent.duration.{FiniteDuration, NANOSECONDS}

/**
 * A simple representation of the [[AnalyzedLog]] to keep type safety as much as possible and have straightforward JSON mappings
 */
final case class LogAnalyzerResponse(identifiedAs: String, reason: Option[String], elapsedTimeNs: Long) {
  def toLog: AnalyzedLog = (identifiedAs, reason) match {
    case ("bot", Some(m)) => AnalyzedLog.Bot(m, FiniteDuration(elapsedTimeNs, NANOSECONDS))
    case ("user", None) => AnalyzedLog.User(FiniteDuration(elapsedTimeNs, NANOSECONDS))
    case _ => throw new MatchError(s"Invalid ${this.getClass.getSimpleName}") // should never happen, see `apply`
  }
}

object LogAnalyzerResponse {
  def apply(analyzed: AnalyzedLog): LogAnalyzerResponse = analyzed match {
    case AnalyzedLog.Bot(reason, time) => new LogAnalyzerResponse("bot", Some(reason), time.toNanos)
    case AnalyzedLog.User(time) => new LogAnalyzerResponse("user", None, time.toNanos)
  }

  implicit val encoder: Encoder[LogAnalyzerResponse] = deriveEncoder[LogAnalyzerResponse]
  implicit val decoder: Decoder[LogAnalyzerResponse] = deriveDecoder[LogAnalyzerResponse]

  implicit def entityEncoder[F[_]]: EntityEncoder[F, LogAnalyzerResponse] = jsonEncoderOf[F, LogAnalyzerResponse]

  implicit def entityDecoder[F[_] : Sync]: EntityDecoder[F, LogAnalyzerResponse] = jsonOf[F, LogAnalyzerResponse]
}
