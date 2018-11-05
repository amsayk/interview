package forex.interfaces.api.utils

import akka.http.scaladsl.model.{ HttpEntity, HttpResponse, StatusCodes }
import akka.http.scaladsl._
import forex.processes._

object ApiExceptionHandler {

  def apply(): server.ExceptionHandler =
    server.ExceptionHandler {
      case RatesError.RateNotFound(pair) ⇒
        _.complete(HttpResponse(StatusCodes.NotFound, entity = s"No rates available for quote '${pair.symbol}'."))

      case RatesError.ApiError(message) ⇒
        _.complete(
          HttpResponse(
            StatusCodes.Conflict,
            entity = HttpEntity(message.getOrElse("Something went wrong with 1forge API."))
          )
        )

      case RatesError.System(_) ⇒
        _.complete(HttpResponse(StatusCodes.InternalServerError, entity = "Something went wrong."))

      case _: Throwable ⇒
        _.complete(HttpResponse(StatusCodes.InternalServerError, entity = "Something went wrong."))
    }

}
