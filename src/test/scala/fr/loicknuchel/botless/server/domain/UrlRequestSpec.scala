package fr.loicknuchel.botless.server.domain

import fr.loicknuchel.botless.testingutils.BaseSpec

class UrlRequestSpec extends BaseSpec {
  describe("UrlRequest") {
    it("should extract file from request if exists") {
      UrlRequest("/").file shouldBe None
      UrlRequest("/administrator").file shouldBe None
      UrlRequest("/administrator/").file shouldBe None
      UrlRequest("/administrator/index.php").file shouldBe Some(FileName("index.php"))
      UrlRequest("/administrator/index.php/").file shouldBe Some(FileName("index.php"))
      UrlRequest("/templates/_system/css/general.css").file shouldBe Some(FileName("general.css"))
      UrlRequest("/modules/mod_bowslideshow/tmpl/js/sliderman.1.3.0.js").file shouldBe Some(FileName("sliderman.1.3.0.js"))
      UrlRequest("/index.php?option=com_content&view=article&id=49&Itemid=55").file shouldBe Some(FileName("index.php"))
      UrlRequest("/index.php#home").file shouldBe Some(FileName("index.php"))
      UrlRequest("/apache-log/access-full.log.61.gz").file shouldBe Some(FileName("access-full.log.61.gz"))
    }
  }
}
