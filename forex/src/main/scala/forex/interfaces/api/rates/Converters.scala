package forex.interfaces.api.rates

import forex.domain._
import forex.processes.rates.messages._

object Converters {
  import Protocol._

  def toGetRequest(
      request: GetApiRequest
  ): GetRequest =
    GetRequest(request.from, request.to)

  def toGetApiResponse(
      rate: Rate
  ): GetApiResponse =
    GetApiResponse(
      from = rate.pair.from,
      to = rate.pair.to,
      price = rate.price,
      timestamp = rate.timestamp
    )

  def toErrorApiResponse: Error ⇒ ErrorApiResponse = {
    case Error.RateNotFound(pair) ⇒
      ErrorApiResponse(
        `type` = "Error",
        code = "RateNotFound",
        message = s"No quotes available from ${pair.from} to ${pair.to}."
      )
    case Error.QuotaExceeded ⇒
      ErrorApiResponse(`type` = "Error", code = "QuotaExceeded", message = s"Quota exceeded for the day.")
    case Error.MarketClosed ⇒ ErrorApiResponse(`type` = "Error", code = "MarketClosed", message = s"Market closed.")
    case Error.System(_)    ⇒ ErrorApiResponse(`type` = "Error", code = "System", message = s"Some went wrong.")
  }

}
