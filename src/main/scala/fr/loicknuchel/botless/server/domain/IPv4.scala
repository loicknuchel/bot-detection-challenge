package fr.loicknuchel.botless.server.domain

import cats.data.ValidatedNec
import cats.implicits.{catsSyntaxTuple4Semigroupal, catsSyntaxValidatedIdBinCompat0}
import fr.loicknuchel.botless.server.engine.Parser.ParsingError.InvalidIp
import fr.loicknuchel.botless.server.engine.Parser.ParsingError.InvalidIp.{InvalidIpScheme, NotANumberIPPart, TooBigIPPart}

import scala.util.{Failure, Success, Try}

final case class IPv4(a: Byte, b: Byte, c: Byte, d: Byte) {
  def value: String = s"${a & 0xff}.${b & 0xff}.${c & 0xff}.${d & 0xff}"
}

object IPv4 {
  private val ipRegex = "([^.]+).([^.]+).([^.]+).([^.]+)".r

  def fromString(ip: String): ValidatedNec[InvalidIp, IPv4] = ip match {
    case ipRegex(aStr, bStr, cStr, dStr) => (
      toByte(aStr, 1, ip),
      toByte(bStr, 2, ip),
      toByte(cStr, 3, ip),
      toByte(dStr, 4, ip)).mapN(IPv4(_, _, _, _))
    case _ => InvalidIpScheme(ip).invalidNec
  }

  private def toByte(part: String, index: Int, ip: String): ValidatedNec[InvalidIp, Byte] = Try(part.toInt) match {
    case Failure(_) => NotANumberIPPart(ip, part, index).invalidNec
    case Success(i) if i <= 255 => i.toByte.validNec
    case Success(i) => TooBigIPPart(ip, i, index, 255).invalidNec
  }
}
