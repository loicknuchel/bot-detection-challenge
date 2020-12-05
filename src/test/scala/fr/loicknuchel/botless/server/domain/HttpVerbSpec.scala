package fr.loicknuchel.botless.server.domain

import cats.implicits.catsSyntaxValidatedIdBinCompat0
import fr.loicknuchel.botless.server.domain.HttpVerb.{GET, POST}
import fr.loicknuchel.botless.server.engine.Parser.ParsingError.InvalidHttpVerb
import fr.loicknuchel.botless.testingutils.{BaseSpec, PropertyChecks}

class HttpVerbSpec extends BaseSpec with PropertyChecks {
  describe("HttpVerb") {
    it("should parse an HttpVerb") {
      HttpVerb.fromString("GET") shouldBe GET.validNec
      HttpVerb.fromString("Post") shouldBe POST.validNec
      HttpVerb.fromString("bad") shouldBe InvalidHttpVerb("bad", HttpVerb.all.map(_.name)).invalidNec
    }
    it("should have a valid HttpVerb generator") {
      forAll { verb: HttpVerb =>
        HttpVerb.fromString(verb.name) shouldBe verb.validNec
      }
    }
  }
}
