package fr.loicknuchel.botless.shared.domain

import scala.concurrent.duration.FiniteDuration

sealed trait AnalyzedLog {
  val elapsedTime: FiniteDuration
}

object AnalyzedLog {

  final case class Bot(reason: String, elapsedTime: FiniteDuration) extends AnalyzedLog

  final case class User(elapsedTime: FiniteDuration) extends AnalyzedLog

}
