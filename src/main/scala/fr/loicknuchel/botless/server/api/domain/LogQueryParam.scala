package fr.loicknuchel.botless.server.api.domain

import fr.loicknuchel.botless.shared.domain.RawLog
import org.http4s.QueryParamDecoder
import org.http4s.dsl.impl.QueryParamDecoderMatcher

/**
 * This is the http4s way to parse query params, use for the GET endpoint for convenience
 */
object LogQueryParam extends QueryParamDecoderMatcher[RawLog]("log")(QueryParamDecoder[String].map(RawLog))
