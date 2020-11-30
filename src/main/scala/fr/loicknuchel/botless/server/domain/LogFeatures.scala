package fr.loicknuchel.botless.server.domain

import java.time.OffsetDateTime

/**
 * Minimal set of features used to discriminate bots
 */
final case class LogFeatures(date: OffsetDateTime,
                             ip: IPv4,
                             userAgent: UserAgent,
                             verb: HttpVerb,
                             status: HttpStatus,
                             fileExtension: FileExtension)
