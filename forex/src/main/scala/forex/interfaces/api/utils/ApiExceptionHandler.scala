package forex.interfaces.api.utils

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl._
import akka.http.scaladsl.model.HttpResponse
import forex.processes._

object ApiExceptionHandler {

  def apply(): server.ExceptionHandler =
    server.ExceptionHandler {
      case RatesError.RateNotFound(pair) ⇒
        _.complete(HttpResponse(StatusCodes.NotFound, entity = s"No rates available for quote '${pair.symbol}'."))

      case RatesError.MarketClosed ⇒
        _.complete(HttpResponse(StatusCodes.Conflict, entity = s"Market is closed. Please try again later."))

      case RatesError.QuotaExceeded ⇒
        _.complete(
          HttpResponse(StatusCodes.Conflict, entity = s"Quota is exceeded to today. Please try again tomorrow.")
        )

      case RatesError.System(_) ⇒
        _.complete(HttpResponse(StatusCodes.InternalServerError, entity = "Something went wrong."))

      case _: Throwable ⇒
        _.complete(HttpResponse(StatusCodes.InternalServerError, entity = "Something went wrong."))
    }

}
