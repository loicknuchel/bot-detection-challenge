package fr.loicknuchel.botless.server.domain

import cats.data.ValidatedNec
import cats.implicits.{catsSyntaxOptionId, catsSyntaxValidatedIdBinCompat0}
import fr.loicknuchel.botless.server.engine.Parser.ParsingError.NotANumberResponseSize

import scala.util.Try

final case class ResponseSize(value: Long) extends AnyVal

object ResponseSize {
  def fromString(size: String): ValidatedNec[NotANumberResponseSize, Option[ResponseSize]] =
    if (size == "-") None.validNec
    else Try(ResponseSize(size.toLong)).fold(_ => NotANumberResponseSize(size).invalidNec, _.some.validNec)
}
