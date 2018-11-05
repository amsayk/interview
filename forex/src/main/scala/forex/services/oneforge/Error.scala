package forex.services.oneforge

import forex.domain.Rate

import scala.util.control.NoStackTrace

sealed trait Error extends Throwable with NoStackTrace
object Error {
  final case class RateNotFound(pair: Rate.Pair) extends Error
  final case class OneForgeApiError(message: Option[String]) extends Error
  final case class System(underlying: Throwable) extends Error
}
