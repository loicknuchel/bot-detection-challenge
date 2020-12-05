package fr.loicknuchel.botless.server.domain

import cats.data.ValidatedNec
import cats.implicits.catsSyntaxValidatedIdBinCompat0
import fr.loicknuchel.botless.server.engine.Parser.ParsingError.InvalidHttpVerb

sealed trait HttpVerb extends Product with Serializable {
  def name: String = toString
}

object HttpVerb {

  final case object GET extends HttpVerb

  final case object POST extends HttpVerb

  final case object PUT extends HttpVerb

  final case object PATCH extends HttpVerb

  final case object DELETE extends HttpVerb

  final case object HEAD extends HttpVerb

  final case object OPTIONS extends HttpVerb

  final case object TRACE extends HttpVerb

  final case object CONNECT extends HttpVerb

  val all = Set(GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS, TRACE, CONNECT)

  def fromString(verb: String): ValidatedNec[InvalidHttpVerb, HttpVerb] =
    all
      .find(_.name.toLowerCase == verb.toLowerCase)
      .fold(InvalidHttpVerb(verb, all.map(_.name)).invalidNec[HttpVerb])(_.validNec)
}
