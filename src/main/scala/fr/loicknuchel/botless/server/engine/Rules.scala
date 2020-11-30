package fr.loicknuchel.botless.server.engine

import fr.loicknuchel.botless.server.domain.{FileExtension, LogFeatures}
import fr.loicknuchel.botless.server.engine.ActorAnalyzer.State
import fr.loicknuchel.botless.server.engine.Rule._

final case class Rules(value: List[Rule]) extends AnyVal {
  // TODO do analysis in parallel using IO.race for example
  def analyze(state: State, log: LogFeatures): Option[Rule.BotFound] = {
    // as 'orElse' take the parameter by name, we evaluate rules only until a first one match, it's nice for performance!
    value.foldLeft(Option.empty[Rule.BotFound])(_ orElse _.isBot(state, log))
  }
}

object Rules {
  val defaults: Rules = Rules(List(
    ForbiddenKeywordInUserAgent(Set("bot", "spider", "crawler")),
    UrlInUserAgent,
    MalformedUserAgent,
    PostRatio(5, 0.2),
    DoNotRequestAssets(5, Set("js", "css", "jpg", "png", "gif", "ico").map(FileExtension))))
}
