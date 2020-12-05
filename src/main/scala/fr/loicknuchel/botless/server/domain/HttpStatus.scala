package fr.loicknuchel.botless.server.domain

import cats.data.ValidatedNec
import cats.implicits.catsSyntaxValidatedIdBinCompat0
import fr.loicknuchel.botless.server.engine.Parser.ParsingError.InvalidResponseStatus
import fr.loicknuchel.botless.server.engine.Parser.ParsingError.InvalidResponseStatus.{NotANumberResponseStatus, NotInRangeResponseStatus}

import scala.util.Try

final case class HttpStatus(value: Int) extends AnyVal

object HttpStatus {
  val (min, max) = (100, 599)

  def fromString(status: String): ValidatedNec[InvalidResponseStatus, HttpStatus] =
    Try(status.toInt).toEither match {
      case Left(_) => NotANumberResponseStatus(status).invalidNec
      case Right(s) if min <= s && s <= max => HttpStatus(s).validNec
      case Right(s) => NotInRangeResponseStatus(s, min, max).invalidNec
    }
}
