package fr.loicknuchel.botless.server.domain

import cats.implicits.catsSyntaxValidatedIdBinCompat0
import fr.loicknuchel.botless.server.engine.Parser.ParsingError.InvalidResponseStatus.{NotANumberResponseStatus, NotInRangeResponseStatus}
import fr.loicknuchel.botless.testingutils.BaseSpec

class HttpStatusSpec extends BaseSpec {
  describe("HttpStatus") {
    it("should parse the response status") {
      HttpStatus.fromString("200") shouldBe HttpStatus(200).validNec
      HttpStatus.fromString("12") shouldBe NotInRangeResponseStatus(12, 100, 600).invalidNec
      HttpStatus.fromString("bad") shouldBe NotANumberResponseStatus("bad").invalidNec
    }
  }
}
