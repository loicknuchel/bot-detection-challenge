package fr.loicknuchel.botless.shared.utils

import fr.loicknuchel.botless.testingutils.BaseSpec

class StringUtilsSpec extends BaseSpec {
  describe("StringUtils") {
    it("should pad strings") {
      StringUtils.padRight("hello", 10) shouldBe "hello     "
      StringUtils.padRight("hellohello", 10) shouldBe "hellohello"
      StringUtils.padRight("hello hello", 10) shouldBe "hello h..."

      StringUtils.padLeft("hello", 10) shouldBe "     hello"
      StringUtils.padLeft("hellohello", 10) shouldBe "hellohello"
      StringUtils.padLeft("hello hello", 10) shouldBe "hello h..."
    }
    it("should pad longs") {
      StringUtils.pad(12, 5) shouldBe "   12"
      StringUtils.pad(12345, 5) shouldBe "12345"
      StringUtils.pad(1234567, 5) shouldBe "1234567" // do not truncate numbers when they exceed
    }
  }
}
