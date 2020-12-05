package fr.loicknuchel.botless.server.engine

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import cats.data.ValidatedNec
import fr.loicknuchel.botless.server.domain._
import fr.loicknuchel.botless.server.engine.ActorAnalyzer.State.{Stats, StatsByIp}
import fr.loicknuchel.botless.server.engine.Parser.ParsingError
import fr.loicknuchel.botless.shared.domain.RawLog
import fr.loicknuchel.botless.shared.utils.Extensions.{RichMap, RichMapLong}

import scala.concurrent.duration.DurationInt

/**
 * Where the magic happen...
 *
 * This actor perform the log analysis given the set of rules it has.
 * It has the responsibility of updating its state to allow rules to take decisions on it.
 */
object ActorAnalyzer {
  // TODO put theses in config file
  private[engine] val timeResolution = 30.minute // size of time intervals to compute stats
  private[engine] val timeRetention = 12.hour // how long we keep history

  // keep internal representation private so Rules only rely on semantic accessors, will help maintenance over large set of rules
  final case class State(private[engine] val timeseries: Map[Interval, Stats] = Map()) {
    private[engine] def evolve(logVal: ValidatedNec[ParsingError, LogFeatures]): State =
      logVal.fold(_ => this, log => {
        val retentionDeadline = log.date.minusSeconds(timeRetention.toSeconds)
        val retained = timeseries.filterNot(_._1.isBefore(retentionDeadline)) // remove outdated stats
        val interval = retained.find(_._1.contains(log.date)).map(_._1).getOrElse(Interval(log.date, timeResolution))
        State(retained.updateValue(interval)(_.evolve(log))(Stats()))
      })

    def getCount(ip: IPv4): Long = sum(ip, _.count)

    def getCount(ip: IPv4, v: HttpVerb): Long = sum(ip, _.verbCount.getOrElse(v, 0L))

    def getCount(ip: IPv4, v: HttpStatus): Long = sum(ip, _.statusCount.getOrElse(v, 0L))

    def getCount(ip: IPv4, v: FileExtension): Long = sum(ip, _.extensionCount.getOrElse(v, 0L))

    private def sum(ip: IPv4, f: StatsByIp => Long): Long = sum(_.ips.get(ip).map(f).getOrElse(0L))

    private def sum(f: Stats => Long): Long = timeseries.foldLeft(0L) { case (acc, (_, stats)) => acc + f(stats) }
  }

  object State {

    // log statistics on a given time interval
    final case class Stats(count: Long = 0,
                           ips: Map[IPv4, StatsByIp] = Map()) {
      def evolve(log: LogFeatures): Stats = copy(
        count = count + 1,
        ips = ips.updateValue(log.ip)(_.evolve(log))(StatsByIp()))
    }

    // log statistics on a given time interval and IP
    final case class StatsByIp(count: Long = 0,
                               verbCount: Map[HttpVerb, Long] = Map(),
                               statusCount: Map[HttpStatus, Long] = Map(),
                               extensionCount: Map[FileExtension, Long] = Map()) {
      def evolve(log: LogFeatures): StatsByIp = copy(
        count = count + 1,
        verbCount = verbCount.increment(log.verb),
        statusCount = statusCount.increment(log.status),
        extensionCount = extensionCount.increment(log.fileExtension))
    }

  }

  final case class Command(log: RawLog, replyTo: ActorRef[Reply])

  final case class Reply(result: Option[Rule.BotFound])

  def apply(rules: Rules, state: State = State()): Behaviors.Receive[Command] =
    Behaviors.receive[Command] { case (_, Command(log, replyTo)) =>
      val (_, newState, result) = compute(rules, state, log)
      replyTo ! Reply(result)
      apply(rules, newState)
    }

  def compute(rules: Rules, state: State, log: RawLog): (Option[ParsedLog], State, Option[Rule.BotFound]) = {
    val parsedVal = Parser.parse(log)
    val featuresVal = parsedVal.map(_.toFeatures)
    val newState = state.evolve(featuresVal)
    val result = featuresVal.fold(
      errors => Some(Rule.BotFound(s"Invalid log: ${errors.map(_.getMessage).toChain.toList.mkString(", ")}")),
      features => rules.analyze(newState, features))
    (parsedVal.toOption, newState, result)
  }
}
