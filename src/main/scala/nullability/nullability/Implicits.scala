/*
 * This file was generated by Guardrail (https://github.com/twilio/guardrail).
 * Modifications will be overwritten; instead edit the OpenAPI/Swagger spec file.
 */
package nullability
import cats.syntax.either._
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
import cats.implicits._
import cats.implicits._
import cats.data.EitherT
object Implicits {
  abstract class AddArg[T] { def addArg(key: String, v: T): String }
  object AddArg {
    def build[T](f: String => T => String): AddArg[T] = new AddArg[T] { def addArg(key: String, v: T): String = f(key)(v) }
    implicit def addArgSeq[T](implicit ev: AddArg[T]): AddArg[List[T]] = build[List[T]](key => vs => vs.map(v => ev.addArg(key, v)).mkString("&"))
    implicit def addArgIterable[T](implicit ev: AddArg[T]): AddArg[Iterable[T]] = build[Iterable[T]](key => vs => vs.map(v => ev.addArg(key, v)).mkString("&"))
    implicit def addArgOption[T](implicit ev: AddArg[T]): AddArg[Option[T]] = build[Option[T]](key => v => v.map(ev.addArg(key, _)).getOrElse(""))
  }
  abstract class AddPath[T] { def addPath(v: T): String }
  object AddPath { def build[T](f: T => String): AddPath[T] = new AddPath[T] { def addPath(v: T): String = f(v) } }
  abstract class Show[T] { def show(v: T): String }
  object Show {
    def build[T](f: T => String): Show[T] = new Show[T] { def show(v: T): String = f(v) }
    implicit val showString = build[String](Predef.identity)
    implicit val showInt = build[Int](_.toString)
    implicit val showLong = build[Long](_.toString)
    implicit val showFloat = build[Float](_.toString)
    implicit val showDouble = build[Double](_.toString)
    implicit val showBigInt = build[BigInt](_.toString)
    implicit val showBigDecimal = build[BigDecimal](_.toString)
    implicit val showBoolean = build[Boolean](_.toString)
    implicit val showLocalDate = build[java.time.LocalDate](_.format(java.time.format.DateTimeFormatter.ISO_DATE))
    implicit val showOffsetDateTime = build[java.time.OffsetDateTime](_.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    implicit val showJavaURL = build[java.net.URI](_.toString)
    implicit val showUUID = build[java.util.UUID](_.toString)
  }
  object Formatter {
    def show[T](value: T)(implicit ev: Show[T]): String = {
      ev.show(value)
    }
    def addArg[T](key: String, value: T)(implicit ev: AddArg[T]): String = {
      s"&${ev.addArg(key, value)}"
    }
    def addPath[T](value: T)(implicit ev: AddPath[T]): String = {
      ev.addPath(value)
    }
  }
  class Base64String(val data: Array[Byte]) extends AnyVal { override def toString() = "Base64String(" + data.toString() + ")" }
  object Base64String {
    def apply(bytes: Array[Byte]): Base64String = new Base64String(bytes)
    def unapply(value: Base64String): Option[Array[Byte]] = Some(value.data)
    private[this] val encoder = java.util.Base64.getEncoder
    implicit val encode: Encoder[Base64String] = Encoder[String].contramap[Base64String](v => new String(encoder.encode(v.data)))
    private[this] val decoder = java.util.Base64.getDecoder
    implicit val decode: Decoder[Base64String] = Decoder[String].emapTry(v => scala.util.Try(decoder.decode(v))).map(new Base64String(_))
  }
}