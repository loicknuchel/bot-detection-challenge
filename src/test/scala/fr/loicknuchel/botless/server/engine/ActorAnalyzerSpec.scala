package fr.loicknuchel.botless.server.engine

import java.time.OffsetDateTime

import cats.data.Validated.Valid
import fr.loicknuchel.botless.server.domain.HttpVerb.{GET, POST}
import fr.loicknuchel.botless.server.domain.{FileExtension, HttpStatus}
import fr.loicknuchel.botless.server.engine.ActorAnalyzer.State.{Stats, StatsByIp}
import fr.loicknuchel.botless.server.engine.ActorAnalyzer.{State, timeResolution, timeRetention}
import fr.loicknuchel.botless.testingutils.BaseSpec
import fr.loicknuchel.botless.testingutils.Fixtures.{ip, log}

class ActorAnalyzerSpec extends BaseSpec {
  private val (s200, s300, s404) = (HttpStatus(200), HttpStatus(300), HttpStatus(404))
  private val (php, css, html) = (FileExtension("php"), FileExtension("css"), FileExtension("html"))
  private val (ip1, ip2) = (ip(1, 2, 3, 4), ip(2, 3, 4, 5))

  describe("ActorAnalyzer") {
    describe("State") {
      it("should compute the state and manage retention") {
        val s = State()

        val now = OffsetDateTime.now()
        val s1 = s.evolve(Valid(log(date = now, ip = ip1, verb = GET)))
        s1.timeseries.size shouldBe 1
        s1.getCount(ip1, GET) shouldBe 1

        val sameInterval = now.plusSeconds(timeResolution.toSeconds / 2)
        val s2 = s1.evolve(Valid(log(date = sameInterval, ip = ip1, verb = GET)))
        s1.timeseries.size shouldBe 1
        s2.getCount(ip1, GET) shouldBe 2

        val otherInterval = now.plusSeconds(timeResolution.toSeconds * 2)
        val s3 = s2.evolve(Valid(log(date = otherInterval, ip = ip1, verb = GET)))
        s3.timeseries.size shouldBe 2
        s3.getCount(ip1, GET) shouldBe 3

        val afterRetention = now.plusSeconds(timeRetention.toSeconds + timeResolution.toSeconds + 5)
        val s4 = s3.evolve(Valid(log(date = afterRetention, ip = ip1, verb = GET)))
        s4.timeseries.size shouldBe 2
        s4.getCount(ip1, GET) shouldBe 2
      }
      it("should retrieve state stats") {
        val now = OffsetDateTime.now()
        val next = now.plusSeconds(timeResolution.toSeconds * 2)
        val after = next.plusSeconds(timeResolution.toSeconds * 2)
        val s = List(
          log(date = now, ip = ip1, verb = GET, status = s200, fileExtension = html),
          log(date = now, ip = ip1, verb = POST, status = s300, fileExtension = css),
          log(date = next, ip = ip1, verb = GET, status = s300, fileExtension = php),
          log(date = after, ip = ip1, verb = GET, status = s200, fileExtension = css),
          log(date = after, ip = ip2, verb = GET, status = s200, fileExtension = css)
        ).foldLeft(State())((s, l) => s.evolve(Valid(l)))
        s.getCount(ip1) shouldBe 4
        s.getCount(ip2) shouldBe 1
        s.getCount(ip1, GET) shouldBe 3
        s.getCount(ip1, POST) shouldBe 1
        s.getCount(ip2, GET) shouldBe 1
        s.getCount(ip2, POST) shouldBe 0
        s.getCount(ip1, s200) shouldBe 2
        s.getCount(ip1, s300) shouldBe 2
        s.getCount(ip1, css) shouldBe 2
        s.getCount(ip1, php) shouldBe 1
        s.getCount(ip1, html) shouldBe 1
      }
      describe("Stats") {
        it("should count requests") {
          val s = Stats()
          val s1 = s.evolve(log())
          s1.count shouldBe 1
          val s2 = s1.evolve(log())
          s2.count shouldBe 2
        }
        it("should do stats by IP") {
          val s = Stats()
          val l1 = log(ip = ip1)
          val s1 = s.evolve(l1)
          s1.ips.get(ip1) shouldBe Some(StatsByIp().evolve(l1))
          s1.ips.get(ip2) shouldBe None
          val l2 = log(ip = ip1)
          val s2 = s1.evolve(l2)
          s2.ips.get(ip1) shouldBe Some(StatsByIp().evolve(l1).evolve(l2))
          s2.ips.get(ip2) shouldBe None
          val l3 = log(ip = ip2)
          val s3 = s2.evolve(l3)
          s3.ips.get(ip1) shouldBe Some(StatsByIp().evolve(l1).evolve(l2))
          s3.ips.get(ip2) shouldBe Some(StatsByIp().evolve(l3))
        }
      }
      describe("StatsByIp") {
        it("should count requests") {
          val s = StatsByIp()
          val s1 = s.evolve(log())
          s1.count shouldBe 1
          val s2 = s1.evolve(log())
          s2.count shouldBe 2
        }
        it("should count verb") {
          val s = StatsByIp()
          val s1 = s.evolve(log(verb = GET))
          s1.verbCount.getOrElse(GET, 0) shouldBe 1
          s1.verbCount.getOrElse(POST, 0) shouldBe 0
          val s2 = s1.evolve(log(verb = GET))
          s2.verbCount.getOrElse(GET, 0) shouldBe 2
          s2.verbCount.getOrElse(POST, 0) shouldBe 0
          val s3 = s2.evolve(log(verb = POST))
          s3.verbCount.getOrElse(GET, 0) shouldBe 2
          s3.verbCount.getOrElse(POST, 0) shouldBe 1
        }
        it("should count status") {
          val s = StatsByIp()
          val s1 = s.evolve(log(status = s200))
          s1.statusCount.getOrElse(s200, 0) shouldBe 1
          s1.statusCount.getOrElse(s404, 0) shouldBe 0
          val s2 = s1.evolve(log(status = s404))
          s2.statusCount.getOrElse(s200, 0) shouldBe 1
          s2.statusCount.getOrElse(s404, 0) shouldBe 1
          val s3 = s2.evolve(log(status = s200))
          s3.statusCount.getOrElse(s200, 0) shouldBe 2
          s3.statusCount.getOrElse(s404, 0) shouldBe 1
        }
        it("should count extensions") {
          val s = StatsByIp()
          val s1 = s.evolve(log(fileExtension = css))
          s1.extensionCount.getOrElse(css, 0) shouldBe 1
          s1.extensionCount.getOrElse(php, 0) shouldBe 0
          val s2 = s1.evolve(log(fileExtension = css))
          s2.extensionCount.getOrElse(css, 0) shouldBe 2
          s2.extensionCount.getOrElse(php, 0) shouldBe 0
          val s3 = s2.evolve(log(fileExtension = php))
          s3.extensionCount.getOrElse(css, 0) shouldBe 2
          s3.extensionCount.getOrElse(php, 0) shouldBe 1
        }
      }
    }
  }
}
