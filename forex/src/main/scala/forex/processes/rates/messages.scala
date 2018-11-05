package forex.processes.rates

import forex.domain._
import scala.util.control.NoStackTrace

package messages {
  sealed trait Error extends Throwable with NoStackTrace
  object Error {
    final case class RateNotFound(pair: Rate.Pair) extends Error
    final case class ApiError(message: Option[String]) extends Error
    final case class System(underlying: Throwable) extends Error
  }

  final case class GetRequest(
      from: Currency,
      to: Currency
  )
}
