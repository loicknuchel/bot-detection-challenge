package fr.loicknuchel.botless.client

import java.net.URL
import java.nio.file.{Path, Paths}

import cats.effect.{ExitCode, IO, IOApp}
import fs2.Stream
import org.http4s.Uri
import org.http4s.implicits.http4sLiteralsSyntax
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.global

/**
 * Launch this with sbt using `sbt "runMain fr.loicknuchel.botless.client.ClientApp [input] [uri] [output]"`
 */
object ClientApp extends IOApp {
  private val logger = LoggerFactory.getLogger(this.getClass)
  val defaultInput = "src/test/resources/100.log" // generate it using the DemoSpec test "split the log file in smaller ones"
  val defaultUri = uri"http://localhost:8080"
  val defaultOutput = "target/results/output.log"

  override def run(args: List[String]): IO[ExitCode] = {
    val input = buildInput(args.headOption.getOrElse(defaultInput))
    val uri = args.lift(1).map(Uri.unsafeFromString).getOrElse(defaultUri)
    val output = Paths.get(args.lift(2).getOrElse(defaultOutput))
    exec(input, output, uri).compile.drain.as(ExitCode.Success)
  }

  def exec(input: Either[URL, Path], output: Path, uri: Uri): Stream[IO, Unit] = for {
    client <- Client.create[IO](uri, output)(global)
    _ <- input.fold(client.process, client.process).onFinalize(IO(logger.info("Done")))
  } yield ()

  def buildInput(in: String): Either[URL, Path] = in match {
    case "remote" => Left(new URL("http://www.almhuette-raith.at/apache-log/access.log"))
    case "full" => Right(Paths.get("src/main/resources/access.log"))
    case url if url.startsWith("http") => Left(new URL(url))
    case file => Right(Paths.get(file))
  }
}
