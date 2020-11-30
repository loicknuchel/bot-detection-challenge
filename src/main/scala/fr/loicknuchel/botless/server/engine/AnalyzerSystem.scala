package fr.loicknuchel.botless.server.engine

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import cats.effect.{ContextShift, IO, LiftIO, Resource, Sync, Timer}
import cats.implicits.{catsSyntaxApplicativeId, toFunctorOps}
import fr.loicknuchel.botless.shared.domain.AnalyzedLog.{Bot, User}
import fr.loicknuchel.botless.shared.domain.{AnalyzedLog, RawLog}
import fr.loicknuchel.botless.shared.services.Analyzer
import fr.loicknuchel.botless.shared.utils.TimeUtils
import fs2.Stream

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

/**
 * A wrapper around the [[ActorSystem]] to make it easier to integrate with pure FP
 *
 * This is the bridge between the HTTP server handling requests and the Akka actor analyzing logs
 */
class AnalyzerSystem[F[_] : Sync : LiftIO] private(rules: Rules)(ec: ExecutionContext)(implicit timer: Timer[F]) extends Analyzer[F] {
  private implicit val timeout: Timeout = 100.millis
  private implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)
  private implicit val system: ActorSystem[ActorAnalyzer.Command] = ActorSystem(ActorAnalyzer(rules), "log-analyzer")

  def analyze(log: RawLog): F[AnalyzedLog] =
    TimeUtils.measure(IO.fromFuture(IO(system.ask(ActorAnalyzer.Command(log, _)).map(_.result)(ec))).to[F])
      .map { case (res, time) => res.map(b => Bot(b.reason, time)).getOrElse(User(time)) }

  def release(): F[Unit] = system.terminate().pure
}

object AnalyzerSystem {
  def resource[F[_] : Sync : LiftIO](rules: Rules)(ec: ExecutionContext)(implicit timer: Timer[F]): Resource[F, AnalyzerSystem[F]] =
    Resource.make(new AnalyzerSystem[F](rules)(ec).pure)(_.release())

  def stream[F[_] : Sync : LiftIO](rules: Rules)(ec: ExecutionContext)(implicit timer: Timer[F]): Stream[F, AnalyzerSystem[F]] =
    Stream.bracket(new AnalyzerSystem[F](rules)(ec).pure)(_.release())
}
