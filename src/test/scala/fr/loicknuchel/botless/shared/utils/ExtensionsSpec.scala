package fr.loicknuchel.botless.shared.utils

import fr.loicknuchel.botless.shared.utils.Extensions.{RichMap, RichMapLong}
import fr.loicknuchel.botless.testingutils.{BaseSpec, PropertyChecks}

class ExtensionsSpec extends BaseSpec with PropertyChecks {
  describe("RichMap") {
    it("should update a map value or add it if does not exists") {
      Map(1 -> 1).updateOrSet(1)(_ + 1)(0) shouldBe Map(1 -> 2)
      Map(1 -> 1).updateOrSet(2)(_ + 1)(0) shouldBe Map(1 -> 1, 2 -> 0)
    }
    it("should update a map value with a function, starting with init when not present") {
      Map(1 -> 1).updateValue(1)(_ + 1)(0) shouldBe Map(1 -> 2)
      Map(1 -> 1).updateValue(2)(_ + 1)(0) shouldBe Map(1 -> 1, 2 -> 1)
    }
  }
  describe("RichMapLong") {
    it("should increment value") {
      Map("a" -> 1L).increment("a") shouldBe Map("a" -> 2L)
      Map("a" -> 1L).increment("b") shouldBe Map("a" -> 1L, "b" -> 1L)
      forAll { (m: Map[String, Long], s: String) =>
        val total = m.values.sum
        m.increment(s).values.sum shouldBe total + 1
      }
    }
  }
}
