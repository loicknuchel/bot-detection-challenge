package fr.loicknuchel.botless.testingutils

import cats.Applicative
import cats.implicits.catsSyntaxApplicativeId
import fr.loicknuchel.botless.shared.domain.{AnalyzedLog, RawLog}
import fr.loicknuchel.botless.shared.services.Analyzer

class FakeAnalyzer[F[_] : Applicative](f: RawLog => AnalyzedLog) extends Analyzer[F] {
  override def analyze(log: RawLog): F[AnalyzedLog] = f(log).pure
}
