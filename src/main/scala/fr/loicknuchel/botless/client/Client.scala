package fr.loicknuchel.botless.client

import java.net.URL
import java.nio.file.Path

import cats.effect.{Blocker, Concurrent, ConcurrentEffect, ContextShift, Timer}
import cats.implicits.{catsSyntaxApplicativeId, toFunctorOps}
import fr.loicknuchel.botless.client.services.HttpAnalyzer
import fr.loicknuchel.botless.shared.domain.{AnalyzedLog, RawLog}
import fr.loicknuchel.botless.shared.services.Analyzer
import fr.loicknuchel.botless.shared.utils.Extensions.RichLong
import fs2.{Stream, io, text}
import org.http4s._
import org.http4s.client.blaze.BlazeClientBuilder
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
 * A client that read logs from a source (local or remote file) and send them to an [[analyzer]]
 * Results are written in the [[output]] file
 */
class Client[F[_] : Concurrent : ContextShift](analyzer: Analyzer[F], output: Path) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val chunkSize = 4096

  def process(file: Path): Stream[F, Unit] = {
    logger.info(s"Process $file input")
    process(b => io.file.readAll[F](file, b, chunkSize))
  }

  def process(url: URL): Stream[F, Unit] = {
    logger.info(s"Process $url input")
    process(b => io.readInputStream[F](url.openConnection.getInputStream.pure, chunkSize, b, closeAfterUse = true))
  }

  private def process(read: Blocker => Stream[F, Byte]): Stream[F, Unit] = Stream.resource(Blocker[F]).flatMap { blocker =>
    read(blocker)
      .through(text.utf8Decode)
      .through(text.lines)
      .map(RawLog)
      .zipWithScan(0L)((i, _) => i + 1)
      .evalMap { case (log, i) => analyzer.analyze(log).map(result => Client.Result(i, log, result)) }
      .observe(showProgress)
      .through(writeResults(_, output, blocker))
  }

  private def showProgress(s: Stream[F, Client.Result]): Stream[F, Unit] = {
    def shouldDisplay(i: Long): Boolean = i < 10 || (i < 100 && i % 10 == 0) || (i < 1000 && i % 100 == 0) || i % 1000 == 0

    s.collect { case r if shouldDisplay(r.index) => r.print }.evalMap(logger.info(_).pure)
  }

  private def writeResults(s: Stream[F, Client.Result], file: Path, blocker: Blocker): Stream[F, Unit] =
    s.map(_.print)
      .intersperse("\n")
      .through(text.utf8Encode)
      .through(io.file.writeAll(file, blocker))

}

object Client {
  def create[F[_] : ConcurrentEffect : ContextShift](host: Uri, output: Path)(ec: ExecutionContext)(implicit timer: Timer[F]): Stream[F, Client[F]] = for {
    httpClient <- BlazeClientBuilder[F](ec).stream
  } yield new Client(new HttpAnalyzer(host, httpClient), output)

  final case class Result(index: Long, log: RawLog, botFound: Option[String], elapsedTime: FiniteDuration) {
    def print: String = s"${index.pad(7)} ${botFound.fold("[USER]")(_ => s"[BOT] ")} ${elapsedTime.toMillis.pad(3)} ms - ${log.value}${botFound.fold("")(" (reason: " + _ + ")")}"
  }

  object Result {
    def apply(index: Long, log: RawLog, res: AnalyzedLog): Result = res match {
      case AnalyzedLog.Bot(reason, time) => new Result(index, log, Some(reason), time)
      case AnalyzedLog.User(time) => new Result(index, log, None, time)
    }
  }

}
