package fr.loicknuchel.botless.server.domain

import cats.data.NonEmptyChain
import cats.data.Validated.Invalid
import cats.implicits.catsSyntaxValidatedIdBinCompat0
import fr.loicknuchel.botless.server.engine.Parser.ParsingError.InvalidIp.{InvalidIpScheme, NotANumberIPPart, TooBigIPPart}
import fr.loicknuchel.botless.testingutils.BaseSpec

class IPv4Spec extends BaseSpec {
  describe("IPv4") {
    it("should parse an IPv4") {
      IPv4.fromString("1.2.3.4").map(_.value) shouldBe "1.2.3.4".validNec
      IPv4.fromString("1.342.3.4") shouldBe TooBigIPPart("1.342.3.4", 342, 2, 255).invalidNec
      IPv4.fromString("1.2.bad.4") shouldBe NotANumberIPPart("1.2.bad.4", "bad", 3).invalidNec
      IPv4.fromString("bad") shouldBe InvalidIpScheme("bad").invalidNec

      IPv4.fromString("1.342.bad.4") shouldBe Invalid(NonEmptyChain(
        TooBigIPPart("1.342.bad.4", 342, 2, 255),
        NotANumberIPPart("1.342.bad.4", "bad", 3)))
    }
    it("should print value") {
      IPv4.fromString("0.0.0.0").toOption.get.value shouldBe "0.0.0.0"
      IPv4.fromString("30.120.160.230").toOption.get.value shouldBe "30.120.160.230"
      IPv4.fromString("255.255.255.255").toOption.get.value shouldBe "255.255.255.255"
    }
  }
}
