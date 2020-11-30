package fr.loicknuchel.botless.server.api.domain

import fr.loicknuchel.botless.shared.domain.AnalyzedLog
import fr.loicknuchel.botless.testingutils.BaseSpec

import scala.concurrent.duration.DurationInt

class LogAnalyzerResponseSpec extends BaseSpec {
  describe("LogAnalyzerResponse") {
    it("should convert from/to AnalyzedLog") {
      Seq(
        AnalyzedLog.Bot("reason", 1.nanos),
        AnalyzedLog.User(2.nanos)
      ).foreach { log =>
        LogAnalyzerResponse(log).toLog shouldBe log
      }
    }
  }
}
