package fr.loicknuchel.botless

import java.nio.file.{Files, Paths}
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import fr.loicknuchel.botless.server.domain.{IPv4, ParsedLog}
import fr.loicknuchel.botless.server.engine.{ActorAnalyzer, Rule, Rules}
import fr.loicknuchel.botless.shared.domain.RawLog
import fr.loicknuchel.botless.shared.utils.Extensions.RichString

import scala.io.Source
import scala.util.Try

/**
 * Use this for data exploration and experimentation (similar to `DemoSpec`)
 */
object Exploration {
  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  private val dateSize = format(OffsetDateTime.now()).length
  private val ipHistoryLength = 50
  private val startingIndexForIpToShow = 0
  private val nbIpToShow = 10

  case class Log(index: Int, raw: RawLog, parsed: ParsedLog, bot: Option[Rule.BotFound])

  def main(args: Array[String]): Unit = {
    // val logs = readResource("access.log")
    val logs = analyze(readFile("src/test/resources/100.log"))
    val ips = logs.groupBy(_.parsed.ip)
    println(s"${ips.size} distinct IPs, view from $startingIndexForIpToShow to ${startingIndexForIpToShow + nbIpToShow}:")
    ips.toList.sortBy(-_._2.length).slice(startingIndexForIpToShow, startingIndexForIpToShow + nbIpToShow).foreach { case (ip, history) =>
      println("\n" + formatHistory(ip, history))
    }
    /*
      bots:
        79.142.95.122:  request always the same page, has almost 1 POST for 1 GET, has no referer
        148.251.50.49:  request always the same page, has a POST after every GET, has no referer and no user agent, do 2 request every 1 second
        52.22.118.215:  request very similar files (increment number at the end), request tech files (.log), perform 5 request by sec, has only 404, has no referer, has tech user agent (Python-urllib/1.17)
        37.1.206.196:   request always the same page, has almost 1 POST per GET
        91.200.12.22:   request always the same page, has almost 1 POST per GET, no referer
        198.50.160.104: request always the same page, has only POST, no referer and user agent
        82.80.230.228:  request only images (jpg, gif), no requests on web page, favicon.ico, js or css, no referer
      not bots:
        205.167.170.15:  request many assets, request the /favicon.ico, page requests are spaced (> 10 sec), has mostly 200, has referer (except on first) and user agent
        84.112.161.41:   request many assets, request the /favicon.ico, has mostly 200, has referer and user agent
        213.150.254.81:  request many assets (> 20/1), request the /favicon.ico, web page is "leading" the request "session", has mostly 200, has referer and user agent
        178.191.155.244: idem
        195.212.98.190:  idem
        193.80.30.175:   referer has the path of the last loaded web page (except for /favicon.ico)
     */
  }

  def analyze(logs: List[RawLog]): List[Log] =
    logs.zipWithIndex.foldLeft(ActorAnalyzer.State() -> List.empty[(Int, RawLog, Option[ParsedLog], Option[Rule.BotFound])]) { case ((s, res), (l, i)) =>
      val (p, ns, r) = ActorAnalyzer.compute(Rules.defaults, s, l)
      (ns, (i, l, p, r) :: res)
    }._2.reverse.collect { case (i, l, Some(p), r) => Log(i, l, p, r) }

  def formatHistory(ip: IPv4, history: List[Log]): String = {
    val title = s"History of ${ip.value} (${history.length} requests):"
    val lines = history.sortBy(_.parsed.date).take(ipHistoryLength).groupBy(_.parsed.date).toList.sortBy(_._1).flatMap { case (date, list) =>
      val time = format(date)
      val lines = list.map { case Log(_, _, p, r) =>
        s"${r.fold("[ ]")(_ => "[X]")} ${p.verb.toString.pad(4)} ${p.status.value} ${p.url.resourceType.symbol} ${p.url.value.pad(90)} from ${p.referer.value.pad(50)} with ${p.userAgent.value}${r.fold("")(" (flagged as bot because " + _.reason + ")")}"
      }
      s"$time ${lines.head}" :: lines.tail.map(l => s"${"".pad(dateSize)} $l")
    }
    val footer = if (history.length > ipHistoryLength) s"\n${"".pad(dateSize)} ... truncated history (${history.length - ipHistoryLength} more logs)" else ""
    (title :: lines).mkString("\n") + footer
  }

  def format(d: OffsetDateTime): String = d.format(dateFormatter)

  def readFile(path: String): List[RawLog] = {
    val content = Try(Files.readAllBytes(Paths.get(path))).map(new String(_))
    content.map(_.split("\n").toList.map(RawLog)).getOrElse(List())
  }

  def readResource(file: String): List[RawLog] = {
    val src = Source.fromResource(file)
    val lines = src.getLines().toList.map(RawLog)
    src.close()
    lines
  }
}
