package fr.loicknuchel.botless.shared.services

import fr.loicknuchel.botless.shared.domain.{AnalyzedLog, RawLog}

trait Analyzer[F[_]] {
  def analyze(log: RawLog): F[AnalyzedLog]
}
