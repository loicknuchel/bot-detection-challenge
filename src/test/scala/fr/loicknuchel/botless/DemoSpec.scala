package fr.loicknuchel.botless

import java.nio.file.{Files, Paths}

import cats.data.Validated.{Invalid, Valid}
import fr.loicknuchel.botless.server.engine.Rule.{ForbiddenKeywordInUserAgent, MalformedUserAgent, UrlInUserAgent}
import fr.loicknuchel.botless.server.engine.{ActorAnalyzer, Parser}
import fr.loicknuchel.botless.shared.domain.RawLog
import fr.loicknuchel.botless.shared.utils.Extensions.RichString
import fr.loicknuchel.botless.testingutils.BaseSpec

import scala.io.Source

/**
 * Not a real test suite... More an playground to experiment with code and data...
 */
class DemoSpec extends BaseSpec {
  private lazy val logs = read("1k.log").map(Parser.parse).collect { case Valid(log) => log }

  ignore("should split the log file in smaller ones") {
    val lines = read("access.log").filter(_.value.nonEmpty) // 2 591 485 lines ^^
    Files.write(Paths.get("src/test/resources/10.log"), lines.take(10).map(_.value).mkString("\n").getBytes)
    Files.write(Paths.get("src/test/resources/100.log"), lines.take(100).map(_.value).mkString("\n").getBytes)
    Files.write(Paths.get("src/test/resources/1k.log"), lines.take(1000).map(_.value).mkString("\n").getBytes)
    Files.write(Paths.get("src/test/resources/10k.log"), lines.take(10000).map(_.value).mkString("\n").getBytes)
    Files.write(Paths.get("src/test/resources/100k.log"), lines.take(100000).map(_.value).mkString("\n").getBytes)
    Files.write(Paths.get("src/test/resources/access-full.log"), lines.take(1000000).map(_.value).mkString("\n").getBytes)
  }
  ignore("should do some data stats") {
    println(s"On ${logs.length} logs:")
    val ips = logs.groupBy(_.ip)
    println(s"${ips.size} different ips (max returning: ${ips.values.map(_.length).max})")
    val userAgents = logs.groupBy(_.userAgent)
    println(s"${userAgents.size} different user agents (max used: ${userAgents.values.map(_.length).max})")
    val referers = logs.groupBy(_.referer)
    println(s"${referers.size} different referers (max used: ${referers.values.map(_.length).max})")
    val urls = logs.groupBy(_.url)
    println(s"${urls.size} different urls (max used: ${urls.values.map(_.length).max})")
    val statuses = logs.groupBy(_.status)
    println(s"${statuses.size} different statuses: ${statuses.toList.sortBy(-_._2.length).map { case (s, v) => s"${s.value} (${v.length})" }.mkString(", ")}")
    val protocols = logs.groupBy(_.protocol)
    println(s"${protocols.size} different protocols: ${protocols.toList.sortBy(-_._2.length).map { case (s, v) => s"${s.value} (${v.length})" }.mkString(", ")}")
    val verbs = logs.groupBy(_.verb)
    println(s"${verbs.size} different verbs: ${verbs.toList.sortBy(-_._2.length).map { case (s, v) => s"$s (${v.length})" }.mkString(", ")}")
    val sizes = logs.map(_.size).distinct
    println(s"${sizes.length} different sizes")
    val identities = logs.map(_.identity).distinct
    println(s"${identities.length} different identities: ${identities.mkString(", ")}")
    val usernames = logs.map(_.username).distinct
    println(s"${usernames.length} different usernames: ${usernames.mkString(", ")}")
    val lasts = logs.map(_.last).distinct
    println(s"${lasts.length} different lasts: ${lasts.mkString(", ")}")
  }
  ignore("should list user agents showing how they are filtered") {
    val userAgents = logs.groupBy(_.userAgent).values.map(_.head).toList.sortBy(_.userAgent.value).map(_.toFeatures)
    val state = ActorAnalyzer.State()

    val (keyword, ua1) = userAgents.partition(ForbiddenKeywordInUserAgent(Set("bot", "spider", "crawler")).isBot(state, _).isDefined)
    val (url, ua2) = ua1.partition(UrlInUserAgent.isBot(state, _).isDefined)
    val (malformed, others) = ua2.partition(MalformedUserAgent.isBot(state, _).isDefined)

    println(s"Valid user agents (${others.length}):")
    others.map(_.userAgent.value).foreach(println)
    println(s"\nkeyword filtered user agents (${keyword.length}):")
    keyword.map(_.userAgent.value).foreach(println)
    println(s"\nurl filtered user agents (${url.length}):")
    url.map(_.userAgent.value).foreach(println)
    println(s"\nmalformed user agents (${malformed.length}):")
    malformed.map(_.userAgent.value).foreach(println)
  }
  ignore("should list referers") {
    val referers = logs.groupBy(_.referer)
    println(s"Referers (${referers.size}):")
    referers.toList.sortBy(-_._2.length).foreach { case (referer, l) => println(s"${referer.value} (${l.length})") }
  }
  ignore("should list urls") {
    val urls = logs.groupBy(_.url)
    println(s"Urls (${urls.size}):")
    urls.toList.sortBy(_._1.value).foreach { case (url, _) =>
      println(s"${url.value.pad(120)} ${url.path.value.pad(50)} ${url.file.map(_.value).getOrElse("").pad(20)} ${url.fileExtension.map(_.value).getOrElse("").pad(4)} ${url.resourceType}")
    }
  }
  ignore("should count user agents by ip") {
    val userAgentsByIp = logs.groupBy(_.ip).view.mapValues(_.groupBy(_.userAgent))
    val multipleUserAgents = userAgentsByIp.mapValues(_.keys.toList).filter(_._2.length > 1).toList.sortBy(-_._2.length)
    println(s"${multipleUserAgents.length} IPs have multiple user agents (on a total of ${userAgentsByIp.size}):")
    multipleUserAgents.foreach { case (ip, userAgents) =>
      println(s"${userAgents.size} user agents used by ${ip.value}: ${userAgents.map(_.value).mkString(", ")}")
    }
  }
  ignore("should correctly parse a file") {
    val lines = read("1k.log")
    val errors = lines.filter(_.value.nonEmpty).map(Parser.parse).collect { case Invalid(err) => err }
    println(s"${lines.length} lines, ${errors.length} errors")
    errors.length shouldBe 0
  }

  private def read(file: String): List[RawLog] = {
    val src = Source.fromResource(file)
    val lines = src.getLines().toList.map(RawLog)
    src.close()
    lines
  }
}
