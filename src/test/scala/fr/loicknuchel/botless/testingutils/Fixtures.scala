package fr.loicknuchel.botless.testingutils

import java.time.OffsetDateTime

import fr.loicknuchel.botless.server.domain.HttpVerb.GET
import fr.loicknuchel.botless.server.domain._

object Fixtures {
  private val now = OffsetDateTime.now()
  private val ip = IPv4(6, 7, 8, 9)
  private val userAgent = "Mozilla/5.0 (Windows NT 6.0; rv:34.0) Gecko/20100101 Firefox/34.0"

  def ip(a: Int, b: Int, c: Int, d: Int): IPv4 = IPv4(a.toByte, b.toByte, c.toByte, d.toByte)

  def log(date: OffsetDateTime = now,
          ip: IPv4 = ip,
          userAgent: String = userAgent,
          verb: HttpVerb = GET,
          status: HttpStatus = HttpStatus(201),
          fileExtension: FileExtension = FileExtension("html")): LogFeatures =
    LogFeatures(date, ip, UserAgent(userAgent), verb, status, fileExtension)
}
