package fr.loicknuchel.botless.server.api.domain

import fr.loicknuchel.botless.shared.domain.AnalyzedLog
import fr.loicknuchel.botless.testingutils.{BaseSpec, PropertyChecks}

import scala.concurrent.duration.DurationInt

class LogAnalyzerResponseSpec extends BaseSpec with PropertyChecks {
  describe("LogAnalyzerResponse") {
    it("should convert from/to AnalyzedLog") {
      Seq(
        AnalyzedLog.Bot("reason", 1.nanos),
        AnalyzedLog.User(2.nanos)
      ).foreach { log =>
        LogAnalyzerResponse(log).toLog shouldBe log
      }
    }
    it("should have a valid AnalyzedLog generator") {
      forAll { log: AnalyzedLog =>
        LogAnalyzerResponse(log).toLog shouldBe log
      }
    }
  }
}
