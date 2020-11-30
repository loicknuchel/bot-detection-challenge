package fr.loicknuchel.botless.server

import cats.effect.IO
import fr.loicknuchel.botless.shared.domain.{AnalyzedLog, RawLog}
import fr.loicknuchel.botless.testingutils.{BaseSpec, FakeAnalyzer}
import org.http4s.implicits.{http4sKleisliResponseSyntaxOptionT, http4sLiteralsSyntax}
import org.http4s.{Method, Request, Status}

import scala.concurrent.duration.DurationInt

class ServerSpec extends BaseSpec {
  private val analyzer = new FakeAnalyzer[IO]({
    case RawLog("user") => AnalyzedLog.User(0.nanos)
    case RawLog("bot") => AnalyzedLog.Bot("error", 0.nanos)
    case RawLog(value) => AnalyzedLog.Bot(value, 0.nanos)
  })
  private val ctrl = Server.router(analyzer)

  describe("Server") {
    it("should analyze a log using GET method") {
      val req = Request[IO](Method.GET, uri"/api/logs/analyze?log=user")
      val res = ctrl.orNotFound(req).unsafeRunSync()
      res.status shouldBe Status.Ok
      res.as[String].unsafeRunSync() shouldBe "{\"identifiedAs\":\"user\",\"reason\":null,\"elapsedTimeNs\":0}"
    }
    it("should analyze a log using POST method") {
      val req = Request[IO](Method.POST, uri"/api/logs/analyze").withEntity("bot")
      val res = ctrl.orNotFound(req).unsafeRunSync()
      res.status shouldBe Status.Ok
      res.as[String].unsafeRunSync() shouldBe "{\"identifiedAs\":\"bot\",\"reason\":\"error\",\"elapsedTimeNs\":0}"
    }
  }
}
