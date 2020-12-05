package fr.loicknuchel.botless.testingutils

import fr.loicknuchel.botless.server.domain._
import fr.loicknuchel.botless.shared.domain.AnalyzedLog
import org.scalacheck.{Arbitrary, Gen}

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{FiniteDuration, TimeUnit}

/**
 * Define ScalaCheck generators to use in tests.
 *
 * Some generators have multiple implementations (see oneOf) to allow :
 *  - diverse but also repetitive data (ex: IPv4)
 *  - different kind of data (correct, incorrect, long, short...)
 */
object Generators {
  // useful constants
  private val frequentIps = List("95.29.198.15", "109.184.11.34").map(IPv4.fromString(_).toOption.get)
  private val frequentHttpVerbs = List(HttpVerb.GET, HttpVerb.POST)
  private val fileExtensionSamples = List("js", "css", "jpg", "png", "php", "gif", "ico", "html", "log", "txt", "zip", "rar", "xml", "json").map(FileExtension)
  private val frequentFileExtensions = List("js", "css", "jpg", "png", "php").map(FileExtension)
  private val frequentUserAgents = List(
    "Mozilla/4.0 (compatible; MSIE 5.01; Windows NT 5.0)",
    "Mozilla/5.0 (Windows NT 6.0; rv:34.0) Gecko/20100101 Firefox/34.0",
    "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.65 Safari/537.36",
    "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)").map(UserAgent)

  // generic generators
  implicit val aByte: Arbitrary[Byte] = Arbitrary.arbByte
  implicit val aLong: Arbitrary[Long] = Arbitrary.arbLong
  implicit val aString: Arbitrary[String] = oneOf(Gen.alphaStr, Gen.asciiStr, Arbitrary.arbString.arbitrary)
  implicit val aInstant: Arbitrary[Instant] = Arbitrary(Gen.calendar.map(_.toInstant))
  implicit val aZoneOffset: Arbitrary[ZoneOffset] = Arbitrary(Gen.oneOf(ZoneOffset.UTC, ZoneOffset.MIN, ZoneOffset.MAX))
  implicit val aOffsetDateTime: Arbitrary[OffsetDateTime] = arb(aInstant, aZoneOffset, (i: Instant, o: ZoneOffset) => i.atOffset(o))
  implicit val aTimeUnit: Arbitrary[TimeUnit] = Arbitrary(Gen.oneOf(TimeUnit.values().toList))
  implicit val aFiniteDuration: Arbitrary[FiniteDuration] = oneOf(arb(Gen.chooseNum[Long](0, 10), aTimeUnit.arbitrary, new FiniteDuration(_, _)), Arbitrary(Gen.finiteDuration))

  // domain generators
  implicit val aIPv4: Arbitrary[IPv4] = oneOf(arb(frequentIps), arb(aByte, aByte, aByte, aByte, IPv4(_, _, _, _)))
  implicit val aHttpVerb: Arbitrary[HttpVerb] = oneOf(arb(frequentHttpVerbs), Arbitrary(Gen.oneOf(HttpVerb.all)))
  implicit val aHttpStatus: Arbitrary[HttpStatus] = arb(Gen.chooseNum(HttpStatus.min, HttpStatus.max, 200, 404, 500), HttpStatus(_))
  implicit val aFileExtension: Arbitrary[FileExtension] = oneOf(arb(frequentFileExtensions), arb(fileExtensionSamples))
  implicit val aUserAgent: Arbitrary[UserAgent] = oneOf(arb(frequentUserAgents), arb(aString, UserAgent))
  implicit val aResponseSize: Arbitrary[ResponseSize] = arb(aLong, ResponseSize(_))
  implicit val aAnalyzedLogBot: Arbitrary[AnalyzedLog.Bot] = arb(aString, aFiniteDuration, AnalyzedLog.Bot)
  implicit val aAnalyzedLogUser: Arbitrary[AnalyzedLog.User] = arb(aFiniteDuration, AnalyzedLog.User)
  implicit val aAnalyzedLog: Arbitrary[AnalyzedLog] = oneOf(aAnalyzedLogBot.arbitrary, aAnalyzedLogUser.arbitrary)
  implicit val aLogFeatures: Arbitrary[LogFeatures] = arb(aOffsetDateTime, aIPv4, aUserAgent, aHttpVerb, aHttpStatus, aFileExtension, LogFeatures)

  // helper methods
  private def arb[A](items: List[A]): Arbitrary[A] = Arbitrary(Gen.oneOf(items))

  private def arb[A, B](aa: Arbitrary[A], fn: A => B): Arbitrary[B] = arb(aa.arbitrary, fn)

  private def arb[A, B](ga: Gen[A], fn: A => B): Arbitrary[B] = Arbitrary(ga.map(fn))

  private def arb[A, B, C](aa: Arbitrary[A], ab: Arbitrary[B], fn: (A, B) => C): Arbitrary[C] = arb(aa.arbitrary, ab.arbitrary, fn)

  private def arb[A, B, C](ga: Gen[A], gb: Gen[B], fn: (A, B) => C): Arbitrary[C] = Arbitrary(for {
    a <- ga
    b <- gb
  } yield fn(a, b))

  private def arb[A, B, C, D, E](aa: Arbitrary[A], ab: Arbitrary[B], ac: Arbitrary[C], ad: Arbitrary[D], fn: (A, B, C, D) => E): Arbitrary[E] =
    arb(aa.arbitrary, ab.arbitrary, ac.arbitrary, ad.arbitrary, fn)

  private def arb[A, B, C, D, E](ga: Gen[A], gb: Gen[B], gc: Gen[C], gd: Gen[D], fn: (A, B, C, D) => E): Arbitrary[E] = Arbitrary(for {
    a <- ga
    b <- gb
    c <- gc
    d <- gd
  } yield fn(a, b, c, d))

  private def arb[A, B, C, D, E, F, G](aa: Arbitrary[A], ab: Arbitrary[B], ac: Arbitrary[C], ad: Arbitrary[D], ae: Arbitrary[E], af: Arbitrary[F], fn: (A, B, C, D, E, F) => G): Arbitrary[G] =
    arb(aa.arbitrary, ab.arbitrary, ac.arbitrary, ad.arbitrary, ae.arbitrary, af.arbitrary, fn)

  private def arb[A, B, C, D, E, F, G](ga: Gen[A], gb: Gen[B], gc: Gen[C], gd: Gen[D], ge: Gen[E], gf: Gen[F], fn: (A, B, C, D, E, F) => G): Arbitrary[G] = Arbitrary(for {
    a <- ga
    b <- gb
    c <- gc
    d <- gd
    e <- ge
    f <- gf
  } yield fn(a, b, c, d, e, f))

  private def oneOf[A](g1: Gen[A], g2: Gen[A], gn: Gen[A]*): Arbitrary[A] =
    Arbitrary(Gen.oneOf(g1, g2, gn: _*))

  private def oneOf[A](a1: Arbitrary[A], a2: Arbitrary[A], an: Arbitrary[A]*): Arbitrary[A] =
    Arbitrary(Gen.oneOf(a1.arbitrary, a2.arbitrary, an.map(_.arbitrary): _*))
}
