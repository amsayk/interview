package forex.domain

import cats.Eq
import io.circe._
import io.circe.generic.extras.semiauto._

case class Price(value: BigDecimal) extends AnyVal
object Price {
  def apply(value: Integer): Price =
    Price(BigDecimal(value))

  implicit val encoder: Encoder[Price] = deriveUnwrappedEncoder

  implicit val eq: Eq[Price] = Eq.fromUniversalEquals
}
