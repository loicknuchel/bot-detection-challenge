package fr.loicknuchel.botless.server.engine

import cats.data.Validated.Valid
import fr.loicknuchel.botless.server.domain.FileExtension
import fr.loicknuchel.botless.server.domain.HttpVerb.{GET, POST}
import fr.loicknuchel.botless.server.engine.Rule._
import fr.loicknuchel.botless.testingutils.BaseSpec
import fr.loicknuchel.botless.testingutils.Fixtures.{ip, log}

class RuleSpec extends BaseSpec {
  private val state = ActorAnalyzer.State()
  private val (php, css) = (FileExtension("php"), FileExtension("css"))

  describe("Rule") {
    describe("ForbiddenKeywordInUserAgent") {
      it("should identify log having specific keywords in user agent as bots") {
        val rule = ForbiddenKeywordInUserAgent(Set("bot", "scraper"))
        rule.isBot(state, log(userAgent = "this is a bot")) shouldBe Some(BotFound("User agent contains forbidden keyword: bot"))
        rule.isBot(state, log(userAgent = "this is a scraper")) shouldBe Some(BotFound("User agent contains forbidden keyword: scraper"))
        rule.isBot(state, log(userAgent = "this is a scraper bot")) shouldBe Some(BotFound("User agent contains forbidden keyword: bot"))
        rule.isBot(state, log(userAgent = "this is a user")) shouldBe None
      }
    }
    describe("UrlInUserAgent") {
      it("should identify log having an url in user agent as bots") {
        UrlInUserAgent.isBot(state, log(userAgent = "bot ref: http://ex.com")) shouldBe Some(BotFound("User agent contains an url"))
        UrlInUserAgent.isBot(state, log(userAgent = "valid ua")) shouldBe None
      }
    }
    describe("MalformedUserAgent") {
      it("should identify log having escaped chars in user agent as bots") {
        MalformedUserAgent.isBot(state, log(userAgent = "{\\\"}")) shouldBe Some(BotFound("User agent is invalid"))
        MalformedUserAgent.isBot(state, log(userAgent = "valid ua")) shouldBe None
      }
    }
    describe("PostRatio") {
      it("should identify log having an IP doing a lot of POST as bots") {
        val rule = PostRatio(0, 0.5)
        val s1 = state.evolve(Valid(log(verb = POST)))
        rule.isBot(s1, log()) shouldBe Some(BotFound("POST ratio too high for IP (100% while threshold is 50%)"))
      }
      it("should have a detection latency to avoid too high ratio at the beginning") {
        val rule = PostRatio(requestLatency = 2, maxPostRatio = 0.4)
        List(
          log(verb = GET) -> None,
          log(verb = POST) -> None, // request latency protects from too high ratios on the beginning (50% on 2 requests ^^)
          log(verb = GET) -> None,
          log(verb = POST) -> Some(BotFound("POST ratio too high for IP (50% while threshold is 40%)")),
          log(verb = POST) -> Some(BotFound("POST ratio too high for IP (60% while threshold is 40%)")),
          log(verb = POST) -> Some(BotFound("POST ratio too high for IP (67% while threshold is 40%)"))
        ).zipWithIndex.foldLeft(state) { case (s, ((l, r), i)) =>
          val s2 = s.evolve(Valid(l))
          val res = rule.isBot(s2, l)
          if (res != r) fail(s"Invalid result at index $i: expect $r but got $res")
          s2
        }
      }
    }
    describe("DoNotRequestAssets") {
      it("should identify IP not downloading assets as bots") {
        val rule = DoNotRequestAssets(2, Set("css").map(FileExtension))
        val (ip1, ip2) = (ip(1, 2, 3, 4), ip(2, 3, 4, 5))
        List(
          log(ip = ip1, verb = GET, fileExtension = php) -> None,
          log(ip = ip1, verb = GET, fileExtension = php) -> None,
          log(ip = ip1, verb = GET, fileExtension = php) -> Some(BotFound("No asset requested after 2 requests (css)")),
          log(ip = ip1, verb = GET, fileExtension = php) -> Some(BotFound("No asset requested after 2 requests (css)")),
          log(ip = ip2, verb = GET, fileExtension = php) -> None,
          log(ip = ip2, verb = GET, fileExtension = css) -> None,
          log(ip = ip2, verb = GET, fileExtension = php) -> None,
          log(ip = ip2, verb = GET, fileExtension = php) -> None
        ).zipWithIndex.foldLeft(state) { case (s, ((l, r), i)) =>
          val s2 = s.evolve(Valid(l))
          val res = rule.isBot(s2, l)
          if (res != r) fail(s"Invalid result at index $i: expect $r but got $res")
          s2
        }
      }
    }
  }
}
