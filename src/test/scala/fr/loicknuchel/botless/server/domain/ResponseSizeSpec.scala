package fr.loicknuchel.botless.server.domain

import cats.implicits.catsSyntaxValidatedIdBinCompat0
import fr.loicknuchel.botless.server.engine.Parser.ParsingError.NotANumberResponseSize
import fr.loicknuchel.botless.testingutils.{BaseSpec, PropertyChecks}

class ResponseSizeSpec extends BaseSpec with PropertyChecks {
  describe("ResponseSize") {
    it("should parse the response size") {
      ResponseSize.fromString("200") shouldBe Some(ResponseSize(200)).validNec
      ResponseSize.fromString("-") shouldBe None.validNec
      ResponseSize.fromString("bad") shouldBe NotANumberResponseSize("bad").invalidNec
    }
    it("should have a valid ResponseSize generator") {
      forAll { res: Option[ResponseSize] =>
        ResponseSize.fromString(res.map(_.value.toString).getOrElse("-")) shouldBe res.validNec
      }
    }
  }
}
