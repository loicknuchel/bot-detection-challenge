package fr.loicknuchel.botless.testingutils

import com.danielasfregola.randomdatagenerator.RandomDataGenerator
import fr.loicknuchel.botless.server.domain.{FileExtension, HttpStatus, HttpVerb, IPv4}
import fr.loicknuchel.botless.testingutils.Generators._
import org.scalacheck.Arbitrary

import java.time.OffsetDateTime
import scala.reflect.runtime.universe._

trait RandomData {
  protected lazy val List(date1) = randomDistinct[OffsetDateTime](1)
  protected lazy val List(ip1, ip2, ip3, ip4) = randomDistinct[IPv4](4)
  protected lazy val List(status1, status2, status3) = randomDistinct[HttpStatus](3)
  protected lazy val List(verb1, verb2, verb3) = randomDistinct[HttpVerb](3)
  protected lazy val List(ext1, ext2, ext3) = randomDistinct[FileExtension](3)

  private def randomDistinct[T: WeakTypeTag : Arbitrary](n: Int): List[T] =
    RandomDataGenerator.random[T](n * 10).distinct.take(n).toList
}
