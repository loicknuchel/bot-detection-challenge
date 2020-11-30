package fr.loicknuchel.botless.shared.utils

object Extensions {

  implicit class RichString(val value: String) extends AnyVal {
    def pad(s: Int): String = StringUtils.pad(value, s)
  }

  implicit class RichLong(val value: Long) extends AnyVal {
    def pad(s: Int): String = StringUtils.pad(value, s)
  }

  implicit class RichMap[A, B](val value: Map[A, B]) extends AnyVal {
    // update the existing value with the given function or set it with the default
    def updateOrSet(key: A)(f: B => B)(default: => B): Map[A, B] =
      value.updatedWith(key)(v => Some(v.map(f).getOrElse(default)))

    // update the existing value with the given function, using init value is not present
    def updateValue(key: A)(f: B => B)(init: => B): Map[A, B] =
      value.updatedWith(key)(v => Some(v.map(f).getOrElse(f(init))))
  }

  implicit class RichMapLong[A](val value: Map[A, Long]) extends AnyVal {
    def increment(key: A): Map[A, Long] =
      value.updatedWith(key)(v => Some(v.map(_ + 1).getOrElse(1)))
  }

}
