package forex.processes.rates

import forex.services._

package object converters {
  import messages._

  def toProcessError[T <: Throwable](t: T): Error = t match {
    case OneForgeError.RateNotFound(pair)        ⇒ Error.RateNotFound(pair)
    case OneForgeError.OneForgeApiError(message) ⇒ Error.ApiError(message)
    case OneForgeError.System(err)               ⇒ Error.System(err)
    case err                                     ⇒ Error.System(err)
  }

}
