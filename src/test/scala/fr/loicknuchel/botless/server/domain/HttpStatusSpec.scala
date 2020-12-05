package fr.loicknuchel.botless.server.domain

import cats.implicits.catsSyntaxValidatedIdBinCompat0
import fr.loicknuchel.botless.server.engine.Parser.ParsingError.InvalidResponseStatus.{NotANumberResponseStatus, NotInRangeResponseStatus}
import fr.loicknuchel.botless.testingutils.{BaseSpec, PropertyChecks}

class HttpStatusSpec extends BaseSpec with PropertyChecks {
  describe("HttpStatus") {
    it("should parse the response status") {
      HttpStatus.fromString("200") shouldBe HttpStatus(200).validNec
      HttpStatus.fromString("12") shouldBe NotInRangeResponseStatus(12, HttpStatus.min, HttpStatus.max).invalidNec
      HttpStatus.fromString("bad") shouldBe NotANumberResponseStatus("bad").invalidNec
    }
    it("should have a valid HttpStatus generator") {
      forAll { status: HttpStatus =>
        HttpStatus.fromString(status.value.toString) shouldBe status.validNec
      }
    }
  }
}
