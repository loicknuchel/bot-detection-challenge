package fr.loicknuchel.botless.testingutils

import fr.loicknuchel.botless.server.domain._
import fr.loicknuchel.botless.shared.domain.AnalyzedLog
import org.scalacheck.Arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.OffsetDateTime
import scala.concurrent.duration.FiniteDuration

trait PropertyChecks extends ScalaCheckPropertyChecks {
  implicit val aByte: Arbitrary[Byte] = Generators.aByte
  implicit val aLong: Arbitrary[Long] = Generators.aLong
  implicit val aString: Arbitrary[String] = Generators.aString
  implicit val aFiniteDuration: Arbitrary[FiniteDuration] = Generators.aFiniteDuration
  implicit val aOffsetDateTime: Arbitrary[OffsetDateTime] = Generators.aOffsetDateTime

  implicit val aIPv4: Arbitrary[IPv4] = Generators.aIPv4
  implicit val aHttpVerb: Arbitrary[HttpVerb] = Generators.aHttpVerb
  implicit val aHttpStatus: Arbitrary[HttpStatus] = Generators.aHttpStatus
  implicit val aFileExtension: Arbitrary[FileExtension] = Generators.aFileExtension
  implicit val aUserAgent: Arbitrary[UserAgent] = Generators.aUserAgent
  implicit val aResponseSize: Arbitrary[ResponseSize] = Generators.aResponseSize
  implicit val aAnalyzedLogBot: Arbitrary[AnalyzedLog.Bot] = Generators.aAnalyzedLogBot
  implicit val aAnalyzedLogUser: Arbitrary[AnalyzedLog.User] = Generators.aAnalyzedLogUser
  implicit val aAnalyzedLog: Arbitrary[AnalyzedLog] = Generators.aAnalyzedLog
  implicit val aLogFeatures: Arbitrary[LogFeatures] = Generators.aLogFeatures
}
