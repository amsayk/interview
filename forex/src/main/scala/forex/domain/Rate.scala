package forex.domain

import cats.Eq
import cats.syntax.show._
import io.circe._
import io.circe.generic.semiauto._

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  ) {

    def symbol: String = from.show + to.show
  }

  object Pair {
    implicit val encoder: Encoder[Pair] =
      deriveEncoder[Pair]

  }

  implicit val encoder: Encoder[Rate] =
    deriveEncoder[Rate]

  implicit val eq: Eq[Rate] = Eq.fromUniversalEquals

  final case class Cached(rate: Rate, time: Long)
}
