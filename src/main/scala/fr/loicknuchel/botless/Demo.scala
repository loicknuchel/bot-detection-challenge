package fr.loicknuchel.botless

import java.nio.file.Paths

import cats.effect.{ExitCode, IO, IOApp}
import fr.loicknuchel.botless.client.ClientApp
import fr.loicknuchel.botless.server.Server
import fr.loicknuchel.botless.server.engine.Rules

import scala.concurrent.ExecutionContext.global

/**
 * Program entry point
 *
 * It starts the log analyzer server
 * Then create a client that pull a log source (remote or local file)
 * And stream the logs to the server to get bot identification.
 *
 * Some results are displayed in the console but if you want to see them all, you can look at `target/results/output.log` file (default)
 *
 * Launch this with sbt using `sbt "run [input] [output]"`
 */
object Demo extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val input = ClientApp.buildInput(args.headOption.getOrElse(ClientApp.defaultInput))
    val output = Paths.get(args.lift(1).getOrElse(ClientApp.defaultOutput))
    Server.create[IO](Rules.defaults)(global)
      .flatMap(_.use(uri => ClientApp.exec(input, output, uri)))
      .compile.drain.as(ExitCode.Success)
  }
}
