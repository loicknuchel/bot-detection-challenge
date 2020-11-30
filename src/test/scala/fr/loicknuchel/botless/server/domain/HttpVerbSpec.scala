package fr.loicknuchel.botless.server.domain

import cats.implicits.catsSyntaxValidatedIdBinCompat0
import fr.loicknuchel.botless.server.engine.Parser.ParsingError.InvalidHttpVerb
import fr.loicknuchel.botless.testingutils.BaseSpec

class HttpVerbSpec extends BaseSpec {
  describe("HttpVerb") {
    it("should parse an HttpVerb") {
      HttpVerb.fromString("GET") shouldBe HttpVerb.GET.validNec
      HttpVerb.fromString("Post") shouldBe HttpVerb.POST.validNec
      HttpVerb.fromString("bad") shouldBe InvalidHttpVerb("bad", HttpVerb.all.map(_.toString)).invalidNec
    }
  }
}
