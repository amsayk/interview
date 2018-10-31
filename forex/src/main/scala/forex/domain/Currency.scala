package forex.domain

import cats.Show
import io.circe._
import enumeratum.{ Enum, EnumEntry }

sealed trait Currency extends EnumEntry with Product with Serializable

object Currency extends Enum[Currency] {
  final case object AUD extends Currency
  final case object CAD extends Currency
  final case object CHF extends Currency
  final case object EUR extends Currency
  final case object GBP extends Currency
  final case object NZD extends Currency
  final case object JPY extends Currency
  final case object SGD extends Currency
  final case object USD extends Currency

  implicit val show: Show[Currency] = Show.show { _.entryName }

  implicit val encoder: Encoder[Currency] =
    Encoder.instance[Currency] {
      (show.show _).andThen(Json.fromString)
    }

  val values = findValues

}
