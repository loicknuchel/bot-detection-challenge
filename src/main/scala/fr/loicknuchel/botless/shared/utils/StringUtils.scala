package fr.loicknuchel.botless.shared.utils

object StringUtils {
  def padRight(str: String, s: Int, c: Char = ' '): String =
    if (str.length > s) str.take(s - 3) + "..." else str + (c.toString * (s - str.length))

  def padLeft(str: String, s: Int, c: Char = ' '): String =
    if (str.length > s) str.take(s - 3) + "..." else (c.toString * (s - str.length)) + str

  // polymorphism does not work well with default parameters :(
  def pad(str: String, s: Int, c: Char): String = padRight(str, s, c)

  def pad(str: String, s: Int): String = padRight(str, s)

  def pad(i: Long, s: Int, c: Char): String = {
    val str = i.toString
    if (str.length > s) str else (c.toString * (s - str.length)) + str
  }

  def pad(i: Long, s: Int): String = pad(i, s, ' ')
}
