package fr.loicknuchel.botless.server.engine

import fr.loicknuchel.botless.server.domain.HttpVerb.POST
import fr.loicknuchel.botless.server.domain.{FileExtension, LogFeatures}

/**
 * Here is some interesting part ^^
 *
 * The bot detection is based on business rules given to the analyzer.
 * To enhance it, add new rules and give them to the analyzer.
 * The `defaults` value is the ones suggested to inject in the analyzer, but other sets can be used.
 *
 * More rule ideas are at the end of the file if you want to try implementing them ;)
 */
abstract class Rule(val name: String) {
  def isBot(state: ActorAnalyzer.State, log: LogFeatures): Option[Rule.BotFound]
}

object Rule {

  // TODO use ADT for better errors
  final case class BotFound(reason: String)

  // Let's start with the easy win, some bots self declare themselves so it's quite obvious to filter them ^^
  final case class ForbiddenKeywordInUserAgent(forbiddenKeyword: Set[String]) extends Rule(s"Forbidden keywords in user agent (${forbiddenKeyword.mkString(", ")})") {
    override def isBot(state: ActorAnalyzer.State, log: LogFeatures): Option[BotFound] =
      forbiddenKeyword.find(log.userAgent.value.toLowerCase.contains).map(k => BotFound(s"User agent contains forbidden keyword: $k"))
  }

  // I noticed that some bots put url or email in their user agent to inform the webmaster, nice practice, and also very helpful here!
  case object UrlInUserAgent extends Rule("Url in user agent") {
    override def isBot(state: ActorAnalyzer.State, log: LogFeatures): Option[BotFound] =
      if (log.userAgent.value.matches(".*https?://.*")) Some(BotFound("User agent contains an url")) else None
  }

  // Sending a bad user agent (for example with escaped chars) is clearly a sign of a bot!
  case object MalformedUserAgent extends Rule("Malformed user agent") {
    override def isBot(state: ActorAnalyzer.State, log: LogFeatures): Option[BotFound] =
      if (log.userAgent.value.isEmpty || log.userAgent.value.contains("\\\"")) Some(BotFound("User agent is invalid")) else None
  }

  // Bots often want to perform actions so they do a lot of POST, plus they do not download assets so they have few GET... Let's exploit that!
  final case class PostRatio(requestLatency: Int, maxPostRatio: Double) extends Rule(s"POST ratio for IP > ${math.round(maxPostRatio * 100)}%") {
    override def isBot(state: ActorAnalyzer.State, log: LogFeatures): Option[BotFound] = {
      val total = state.getCount(log.ip)
      if (total > requestLatency) { // wait a few requests to have a relevant ratio
        val ratio = state.getCount(log.ip, POST).toDouble / total
        if (ratio > maxPostRatio) {
          Some(BotFound(s"POST ratio too high for IP (${math.round(ratio * 100)}% while threshold is ${math.round(maxPostRatio * 100)}%)"))
        } else {
          None
        }
      } else {
        None
      }
    }
  }

  // Many bots do not download js, css and images, let flag them!
  final case class DoNotRequestAssets(requestLatency: Int, assetExtensions: Set[FileExtension]) extends Rule(s"Has not requested assets (${assetExtensions.map(_.value).mkString(", ")}) after $requestLatency requests") {
    override def isBot(state: ActorAnalyzer.State, log: LogFeatures): Option[BotFound] = {
      val total = state.getCount(log.ip)
      if (total > requestLatency) { // wait a few requests to have a relevant ratio
        val requestedAssets = assetExtensions.map(state.getCount(log.ip, _)).sum
        if (requestedAssets == 0) {
          Some(BotFound(s"No asset requested after $requestLatency requests (${assetExtensions.map(_.value).mkString(", ")})"))
        } else {
          None
        }
      } else {
        None
      }
    }
  }

  // rule: too many time the same url (no variation)
  // rule: no referer or user agent filled in 3 last requests
  // rule: too many user agents used by an ip
  // rule: increase of number of requests for an IP or a user agent (above average of other users)
  // rule: periodic calls from an IP
  // rule: bad referer: not the same than previous call
  // rule: too close requests for an IP (< 1 sec) but ignoring assets
  // rule: too many 404
}
