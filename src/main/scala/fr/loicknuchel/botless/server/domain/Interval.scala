package fr.loicknuchel.botless.server.domain

import java.time.OffsetDateTime

import scala.concurrent.duration.FiniteDuration

final case class Interval(start: OffsetDateTime, end: OffsetDateTime) {
  def isBefore(date: OffsetDateTime): Boolean = date.isAfter(end)

  def isAfter(date: OffsetDateTime): Boolean = date.isBefore(start)

  def contains(date: OffsetDateTime): Boolean = !isBefore(date) && !isAfter(date)
}

object Interval {
  def apply(start: OffsetDateTime, duration: FiniteDuration): Interval = new Interval(start, start.plusSeconds(duration.toSeconds))
}
