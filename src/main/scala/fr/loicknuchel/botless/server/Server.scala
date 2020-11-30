package fr.loicknuchel.botless.server

import cats.effect.{ConcurrentEffect, ExitCode, IO, Sync, Timer}
import cats.implicits._
import fr.loicknuchel.botless.server.api.domain.{LogAnalyzerResponse, LogQueryParam}
import fr.loicknuchel.botless.server.engine.{AnalyzerSystem, Rules}
import fr.loicknuchel.botless.shared.domain.RawLog
import fr.loicknuchel.botless.shared.services.Analyzer
import fs2.Stream
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.server.{ServerBuilder, defaults}
import org.http4s.{HttpRoutes, MediaType, Uri}

import scala.concurrent.ExecutionContext

/**
 * Wrapper around the HTTP server allowing to use it in a friendly way (pure FP ^^)
 */
class Server[F[_] : ConcurrentEffect](uri: Uri, underlying: ServerBuilder[F]) {
  def serve: Stream[F, ExitCode] = underlying.serve

  def use[A](s: Uri => Stream[F, A]): Stream[F, A] = Stream.evals(underlying.resource.use(_ => s(uri).compile.toList))
}

/**
 * Create the server and its dependencies, this is mostly http4s plumbing...
 */
object Server {
  def create[F[_] : ConcurrentEffect](rules: Rules, port: Int = defaults.HttpPort, host: String = defaults.Host)
                                     (ec: ExecutionContext)(implicit timer: Timer[F]): Stream[F, Server[F]] = for {
    uri <- Stream.eval(Uri.fromString(s"http://$host:$port").fold(IO.raiseError, IO.pure).to)
    analyzer <- AnalyzerSystem.stream(rules)(ec)

    httpApp = router(analyzer).orNotFound
    finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

    server = BlazeServerBuilder[F](ec)
      .bindHttp(port, host)
      .withHttpApp(finalHttpApp)
  } yield new Server(uri, server)

  private[server] def router[F[_] : Sync](analyzer: Analyzer[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "api" / "logs" / "analyze" :? LogQueryParam(log) => for {
        analyzed <- analyzer.analyze(log)
        res <- Ok(LogAnalyzerResponse(analyzed))
      } yield res
      case req@POST -> Root / "api" / "logs" / "analyze" => for {
        log <- req.as[String]
        analyzed <- analyzer.analyze(RawLog(log))
        res <- Ok(LogAnalyzerResponse(analyzed))
      } yield res
      case GET -> Root => Ok(
        """<h1>Welcome to bot analyzer!</h1>
          |<p>Use the API to get your logs analyzed</p>
          |<p>Endpoints:</p>
          |<ul>
          |  <li>
          |    <pre>GET /api/logs/analyze?log=your log</pre>
          |    <a href="/api/logs/analyze?log=109.169.248.247%20-%20-%20%5B12%2FDec%2F2015%3A18%3A25%3A11%20%2B0100%5D%20%22GET%20%2Fadministrator%2F%20HTTP%2F1.1%22%20200%204263%20%22-%22%20%22Mozilla%2F5.0%20%28Windows%20NT%206.0%3B%20rv%3A34.0%29%20Gecko%2F20100101%20Firefox%2F34.0%22%20%22-%22">example for user</a> -
          |    <a href="/api/logs/analyze?log=66.249.66.19%20-%20-%20%5B12%2FDec%2F2015%3A19%3A23%3A59%20%2B0100%5D%20%22GET%20%2Findex.php%3Foption%3Dcom_content%26view%3Darticle%26id%3D46%26Itemid%3D54%20HTTP%2F1.1%22%20200%208932%20%22-%22%20%22Mozilla%2F5.0%20%28compatible%3B%20Googlebot%2F2.1%3B%20%2Bhttp%3A%2F%2Fwww.google.com%2Fbot.html%29%22%20%22-%22">example for bot</a>
          |  </li>
          |  <li>
          |    <pre>POST /api/logs/analyze</pre>
          |    with your log as body (String)
          |  </li>
          |</ul>
          |""".stripMargin, `Content-Type`(MediaType.text.html))
    }
  }
}
