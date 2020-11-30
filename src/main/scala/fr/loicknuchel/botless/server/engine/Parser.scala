package fr.loicknuchel.botless.server.engine

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import cats.data.ValidatedNec
import cats.implicits.{catsSyntaxTuple5Semigroupal, catsSyntaxValidatedIdBinCompat0}
import fr.loicknuchel.botless.server.domain._
import fr.loicknuchel.botless.server.engine.Parser.ParsingError._
import fr.loicknuchel.botless.shared.domain.RawLog

import scala.util.Try

object Parser {
  // I use permissive regex to improve error reporting (know which part of the log were incorrect)
  private val ipRegex = "([^ ]+)"
  private val identityRegex = "([^ ]+)"
  private val usernameRegex = "([^ ]+)"
  private val dateRegex = "\\[([^]]+)]"
  private val verbRegex = "([^ ]+)"
  private val urlRegex = "([^ ]+)"
  private val protocolRegex = "([^\"]+)"
  private val requestRegex = "\"" + s"$verbRegex $urlRegex $protocolRegex" + "\""
  private val statusRegex = "([^ ]+)"
  private val sizeRegex = "(-|[^ ]+)"
  private val refererRegex = "\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\""
  private val userAgentRegex = "\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\""
  private val lastRegex = "\"([^\"]+)\""
  private val logLineRegex = s"$ipRegex $identityRegex $usernameRegex $dateRegex $requestRegex $statusRegex $sizeRegex $refererRegex $userAgentRegex $lastRegex".r
  private val dateFormat = "dd/MMM/yyyy:HH:mm:ss Z"
  private val dateFormatter = DateTimeFormatter.ofPattern(dateFormat)

  def parse(log: RawLog): ValidatedNec[ParsingError, ParsedLog] = log.value match {
    case logLineRegex(ip, id, user, date, verb, url, protocol, status, size, referer, userAgent, last) => (
      IPv4.fromString(ip),
      parseDate(date),
      HttpVerb.fromString(verb),
      HttpStatus.fromString(status),
      ResponseSize.fromString(size)
      ).mapN(ParsedLog(_, id, user, _, _, UrlRequest(url), HttpProtocol(protocol), _, _, Referer(referer), UserAgent(userAgent), last))
    case _ => InvalidLog().invalidNec
  }

  private[engine] def parseDate(date: String): ValidatedNec[InvalidDate, OffsetDateTime] =
    Try(OffsetDateTime.parse(date, dateFormatter)).fold(_ => InvalidDate(date, dateFormat).invalidNec, _.validNec)

  // thanks to by name parameter, the message string is computed only if and when the getMessage is called
  sealed abstract class ParsingError(msg: => String) extends Product {
    def getMessage: String = msg
  }

  object ParsingError {

    final case class InvalidLog() extends ParsingError("Invalid log format, can't parse it")

    sealed abstract class InvalidIp(msg: => String) extends ParsingError(msg) with Product

    object InvalidIp {

      final case class InvalidIpScheme(ip: String) extends InvalidIp(s"Invalid IP scheme for '$ip', expecting an IPv4")

      final case class NotANumberIPPart(ip: String, part: String, partIndex: Int) extends InvalidIp(s"Invalid IP '$ip', part $partIndex ($part) is not a number")

      final case class TooBigIPPart(ip: String, part: Int, partIndex: Int, max: Int) extends InvalidIp(s"Invalid IP '$ip', part $partIndex ($part) is greater than $max")

    }

    final case class InvalidDate(date: String, expectedFormat: String) extends ParsingError(s"Invalid date '$date', expecting $expectedFormat format")

    final case class InvalidHttpVerb(verb: String, possibilities: Set[String]) extends ParsingError(s"Invalid HTTP verb: '$verb', expecting one of ${possibilities.mkString(", ")}")

    sealed abstract class InvalidResponseStatus(msg: => String) extends ParsingError(msg) with Product

    object InvalidResponseStatus {

      final case class NotANumberResponseStatus(status: String) extends InvalidResponseStatus(s"Invalid response status: '$status', expecting an integer")

      final case class NotInRangeResponseStatus(status: Int, min: Int, max: Int) extends InvalidResponseStatus(s"Invalid response status: '$status', expected to be between $min and $max")

    }

    final case class NotANumberResponseSize(size: String) extends ParsingError(s"Invalid response size: '$size', expecting an integer or '-'")

  }

}
