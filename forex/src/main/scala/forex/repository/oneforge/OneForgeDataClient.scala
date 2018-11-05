package forex.repository.oneforge

import cats.Eval
import cats.data.EitherT
import cats.effect.Async
import cats.implicits._
import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import com.softwaremill.sttp.circe.asJson
import com.softwaremill.sttp.prometheus.PrometheusBackend
import forex.config.ForexConfig
import forex.domain.Rate
import forex.repository.OneForgeResponse
import forex.services.oneforge.Error
import io.circe._
import io.circe.generic.auto._
import org.zalando.grafter.{Start, StartResult, Stop, StopResult}

final class OneForgeDataClient[F[_]](
    config: ForexConfig,
)(implicit F: Async[F])
    extends OneForgeDataClientAlg[F]
    with Start
    with Stop {

  import OneForgeDataClient._

  private implicit lazy val sttpBackend = PrometheusBackend[F, Nothing](
    AsyncHttpClientCatsBackend[F](
      options = SttpBackendOptions(
        connectionTimeout = config.connectionTimeout,
        proxy = None,
      )
    ),
    requestToHistogramNameMapper = _ ⇒ Some(HistogramName),
    requestToInProgressGaugeNameMapper = _ ⇒ Some(RequestsInProgressGaugeName),
    requestToSuccessCounterMapper = _ ⇒ Some(SuccessCounterName),
    requestToErrorCounterMapper = _ ⇒ Some(ErrorCounterName),
    requestToFailureCounterMapper = _ ⇒ Some(FailureCounterName),
  )

  def quotes(request: List[Rate.Pair]): EitherT[F, Error, List[Rate]] = {
    val pairs = request.map(_.symbol).mkString(",")

    for {
      response ← EitherT.liftF(
        sttp
          .get(uri"https://forex.1forge.com/1.0.3/quotes?pairs=$pairs&api_key=${config.apiKey}")
          .response(asJson[Json])
          .send()
      )

      res ← response.body match {
        case Left(errorMsg)     ⇒ EitherT.leftT[F, List[Rate]](Error.System(new RuntimeException(errorMsg)): Error)
        case Right(Left(error)) ⇒ EitherT.leftT[F, List[Rate]](Error.System(new RuntimeException(error.message)): Error)
        case Right(Right(json)) ⇒
          if (isErrorResponse(json)) {
            val message = json.hcursor
              .get[String]("message")
              .toOption

            EitherT.leftT[F, List[Rate]](Error.OneForgeApiError(message): Error)
          } else {

            json.as[List[OneForgeResponse]] match {
              case Left(error) ⇒
                EitherT.leftT[F, List[Rate]](Error.System(new RuntimeException(error.message)): Error)

              case Right(quotes) ⇒
                quotes
                  .traverse(
                    quote ⇒
                      F.delay(quote.toRate).attemptT.leftMap { e ⇒
                        Error.System(new RuntimeException(e.getMessage)): Error
                    }
                  )

            }

          }

      }

    } yield res
  }

  override def start: Eval[StartResult] =
    StartResult.eval("OneForgeDataClient") {}

  override def stop: Eval[StopResult] =
    StopResult.eval("OneForgeDataClient") {
      sttpBackend.close()
    }
}

object OneForgeDataClient {
  val HistogramName = "oneforge_request_latency"
  val RequestsInProgressGaugeName = "oneforge_requests_in_progress"
  val SuccessCounterName = "oneforge_requests_success_count"
  val ErrorCounterName = "oneforge_requests_error_count"
  val FailureCounterName = "oneforge_requests_failure_count"

  private[oneforge] def isErrorResponse(json: Json): Boolean =
    json.hcursor
      .get[Boolean]("error")
      .toOption
      .getOrElse(false)

}
