package forex.services.oneforge

import forex.domain.Rate

import scala.util.control.NoStackTrace

sealed trait Error extends Throwable with NoStackTrace
object Error {
  final case class RateNotFound(pair: Rate.Pair) extends Error
  final case object QuotaExceeded extends Error
  final case object MarketClosed extends Error
  final case class System(underlying: Throwable) extends Error
}
