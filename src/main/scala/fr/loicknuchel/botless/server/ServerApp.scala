package fr.loicknuchel.botless.server

import cats.effect.{ExitCode, IO, IOApp}
import fr.loicknuchel.botless.server.engine.Rules
import org.http4s.server.defaults

import scala.concurrent.ExecutionContext.global
import scala.util.Try

/**
 * Launch this with sbt using `sbt "runMain fr.loicknuchel.botless.server.ServerApp [port]"`
 */
object ServerApp extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val port = args.headOption.flatMap(p => Try(p.toInt).toOption).getOrElse(defaults.HttpPort)
    Server.create[IO](Rules.defaults, port)(global)
      .flatMap(_.serve)
      .compile.drain.as(ExitCode.Success)
  }
}
