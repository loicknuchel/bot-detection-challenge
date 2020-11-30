package fr.loicknuchel.botless.server.domain

import cats.data.ValidatedNec
import cats.implicits.catsSyntaxValidatedIdBinCompat0
import fr.loicknuchel.botless.server.engine.Parser.ParsingError.InvalidResponseStatus
import fr.loicknuchel.botless.server.engine.Parser.ParsingError.InvalidResponseStatus.{NotANumberResponseStatus, NotInRangeResponseStatus}

import scala.util.Try

final case class HttpStatus(value: Int) extends AnyVal

object HttpStatus {
  def fromString(status: String): ValidatedNec[InvalidResponseStatus, HttpStatus] =
    Try(status.toInt).toEither match {
      case Left(_) => NotANumberResponseStatus(status).invalidNec
      case Right(s) if 100 <= s && s < 600 => HttpStatus(s).validNec
      case Right(s) => NotInRangeResponseStatus(s, 100, 600).invalidNec
    }
}
