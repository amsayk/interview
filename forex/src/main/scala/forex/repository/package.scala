package forex

import java.time.{ Instant, OffsetDateTime, ZoneId }

import forex.domain.{ Currency, Price, Rate, Timestamp }

package object repository {

  final case class OneForgeResponse(
      symbol: String,
      price: Double,
      timestamp: Long,
  ) {

    def toRate: Rate = {
      val (from, to) = symbol.splitAt(3)

      Rate(
        pair = Rate.Pair(
          from = Currency.withNameInsensitive(from),
          to = Currency.withNameInsensitive(to),
        ),
        price = Price(BigDecimal(price)),
        timestamp = Timestamp(OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())),
      )
    }
  }

}
