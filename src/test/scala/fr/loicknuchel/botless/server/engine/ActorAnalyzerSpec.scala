package fr.loicknuchel.botless.server.engine

import cats.data.Validated.Valid
import fr.loicknuchel.botless.server.domain.LogFeatures
import fr.loicknuchel.botless.server.engine.ActorAnalyzer.State.{Stats, StatsByIp}
import fr.loicknuchel.botless.server.engine.ActorAnalyzer.{State, timeResolution, timeRetention}
import fr.loicknuchel.botless.testingutils.Fixtures.log
import fr.loicknuchel.botless.testingutils.{BaseSpec, PropertyChecks, RandomData}

import java.time.OffsetDateTime

class ActorAnalyzerSpec extends BaseSpec with PropertyChecks with RandomData {
  describe("ActorAnalyzer") {
    describe("State") {
      it("should compute the state and manage retention") {
        val s = State()

        val now = OffsetDateTime.now()
        val s1 = s.evolve(Valid(log(date = now, ip = ip1, verb = verb1)))
        s1.timeseries.size shouldBe 1
        s1.getCount(ip1, verb1) shouldBe 1

        val sameInterval = now.plusSeconds(timeResolution.toSeconds / 2)
        val s2 = s1.evolve(Valid(log(date = sameInterval, ip = ip1, verb = verb1)))
        s1.timeseries.size shouldBe 1
        s2.getCount(ip1, verb1) shouldBe 2

        val otherInterval = now.plusSeconds(timeResolution.toSeconds * 2)
        val s3 = s2.evolve(Valid(log(date = otherInterval, ip = ip1, verb = verb1)))
        s3.timeseries.size shouldBe 2
        s3.getCount(ip1, verb1) shouldBe 3

        val afterRetention = now.plusSeconds(timeRetention.toSeconds + timeResolution.toSeconds + 5)
        val s4 = s3.evolve(Valid(log(date = afterRetention, ip = ip1, verb = verb1)))
        s4.timeseries.size shouldBe 2
        s4.getCount(ip1, verb1) shouldBe 2
      }
      it("should retrieve state stats") {
        val now = OffsetDateTime.now()
        val next = now.plusSeconds(timeResolution.toSeconds * 2)
        val after = next.plusSeconds(timeResolution.toSeconds * 2)
        val s = List(
          log(date = now, ip = ip1, verb = verb1, status = status1, fileExtension = ext3),
          log(date = now, ip = ip1, verb = verb2, status = status2, fileExtension = ext2),
          log(date = next, ip = ip1, verb = verb1, status = status2, fileExtension = ext1),
          log(date = after, ip = ip1, verb = verb1, status = status1, fileExtension = ext2),
          log(date = after, ip = ip2, verb = verb1, status = status1, fileExtension = ext2)
        ).foldLeft(State())((s, l) => s.evolve(Valid(l)))
        s.getCount(ip1) shouldBe 4
        s.getCount(ip2) shouldBe 1
        s.getCount(ip1, verb1) shouldBe 3
        s.getCount(ip1, verb2) shouldBe 1
        s.getCount(ip2, verb1) shouldBe 1
        s.getCount(ip2, verb2) shouldBe 0
        s.getCount(ip1, status1) shouldBe 2
        s.getCount(ip1, status2) shouldBe 2
        s.getCount(ip1, ext2) shouldBe 2
        s.getCount(ip1, ext1) shouldBe 1
        s.getCount(ip1, ext3) shouldBe 1
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
        it("should count requests by kind") {
          forAll { logs: List[LogFeatures] =>
            val sameDateLogs = logs.map(_.copy(date = date1))
            val finalState = sameDateLogs.foldLeft(Stats())(_.evolve(_))
            finalState.count shouldBe logs.length
            finalState.ips.values.map(_.count).sum shouldBe logs.length
          }
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
          val s1 = s.evolve(log(verb = verb1))
          s1.verbCount.getOrElse(verb1, 0) shouldBe 1
          s1.verbCount.getOrElse(verb2, 0) shouldBe 0
          val s2 = s1.evolve(log(verb = verb1))
          s2.verbCount.getOrElse(verb1, 0) shouldBe 2
          s2.verbCount.getOrElse(verb2, 0) shouldBe 0
          val s3 = s2.evolve(log(verb = verb2))
          s3.verbCount.getOrElse(verb1, 0) shouldBe 2
          s3.verbCount.getOrElse(verb2, 0) shouldBe 1
        }
        it("should count status") {
          val s = StatsByIp()
          val s1 = s.evolve(log(status = status1))
          s1.statusCount.getOrElse(status1, 0) shouldBe 1
          s1.statusCount.getOrElse(status3, 0) shouldBe 0
          val s2 = s1.evolve(log(status = status3))
          s2.statusCount.getOrElse(status1, 0) shouldBe 1
          s2.statusCount.getOrElse(status3, 0) shouldBe 1
          val s3 = s2.evolve(log(status = status1))
          s3.statusCount.getOrElse(status1, 0) shouldBe 2
          s3.statusCount.getOrElse(status3, 0) shouldBe 1
        }
        it("should count extensions") {
          val s = StatsByIp()
          val s1 = s.evolve(log(fileExtension = ext2))
          s1.extensionCount.getOrElse(ext2, 0) shouldBe 1
          s1.extensionCount.getOrElse(ext1, 0) shouldBe 0
          val s2 = s1.evolve(log(fileExtension = ext2))
          s2.extensionCount.getOrElse(ext2, 0) shouldBe 2
          s2.extensionCount.getOrElse(ext1, 0) shouldBe 0
          val s3 = s2.evolve(log(fileExtension = ext1))
          s3.extensionCount.getOrElse(ext2, 0) shouldBe 2
          s3.extensionCount.getOrElse(ext1, 0) shouldBe 1
        }
        it("should count requests by kind") {
          forAll { logs: List[LogFeatures] =>
            val sameDateLogs = logs.map(_.copy(date = date1))
            val finalState = sameDateLogs.foldLeft(StatsByIp())(_.evolve(_))
            finalState.count shouldBe logs.length
            finalState.verbCount.values.sum shouldBe logs.length
            finalState.statusCount.values.sum shouldBe logs.length
            finalState.extensionCount.values.sum shouldBe logs.length
          }
        }
      }
    }
  }
}
