package fr.loicknuchel.botless.server.domain

import java.time.OffsetDateTime

final case class ParsedLog(ip: IPv4,
                           identity: String,
                           username: String,
                           date: OffsetDateTime,
                           verb: HttpVerb,
                           url: UrlRequest,
                           protocol: HttpProtocol,
                           status: HttpStatus,
                           size: Option[ResponseSize],
                           referer: Referer,
                           userAgent: UserAgent,
                           last: String) { // don't know what is it
  def toFeatures: LogFeatures = LogFeatures(date, ip, userAgent, verb, status, url.fileExtension.getOrElse(FileExtension("")))
}
