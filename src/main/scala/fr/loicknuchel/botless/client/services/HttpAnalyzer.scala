package fr.loicknuchel.botless.client.services

import cats.effect.{Sync, Timer}
import cats.implicits.toFunctorOps
import fr.loicknuchel.botless.server.api.domain.LogAnalyzerResponse
import fr.loicknuchel.botless.shared.domain.{AnalyzedLog, RawLog}
import fr.loicknuchel.botless.shared.services.Analyzer
import fr.loicknuchel.botless.shared.utils.TimeUtils
import org.http4s._
import org.http4s.client.Client

/**
 * A client analyzer that only call an analyze server to get the results
 */
class HttpAnalyzer[F[_]: Sync](host: Uri, httpClient: Client[F])(implicit timer: Timer[F]) extends Analyzer[F] {
  override def analyze(log: RawLog): F[AnalyzedLog] = {
    val req = Request[F](method = Method.POST, uri = host.withPath("/api/logs/analyze")).withEntity(log.value)
    TimeUtils.measure(httpClient.expect[LogAnalyzerResponse](req)).map {
      case (res, time) => res.copy(elapsedTimeNs = time.toNanos) // use client side time instead of server side one
    }.map(_.toLog)
  }
}
