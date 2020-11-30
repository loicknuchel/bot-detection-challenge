package fr.loicknuchel.botless.shared.utils

import cats.FlatMap
import cats.effect.Clock
import cats.syntax.all.{toFlatMapOps, toFunctorOps}

import scala.concurrent.duration.{FiniteDuration, NANOSECONDS}

object TimeUtils {
  def measure[F[_] : FlatMap, A](fa: F[A])(implicit clock: Clock[F]): F[(A, FiniteDuration)] = {
    for {
      start <- clock.monotonic(NANOSECONDS)
      result <- fa
      finish <- clock.monotonic(NANOSECONDS)
    } yield (result, FiniteDuration(finish - start, NANOSECONDS))
  }
}
