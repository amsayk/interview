package forex.repository.oneforge

import cats.Eval
import cats.effect.Async
import cats.implicits._
import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import com.softwaremill.sttp.circe.asJson
import forex.config.ForexConfig
import forex.domain.Rate
import forex.repository.OneForgeResponse
import forex.services.oneforge.Error
import io.circe._
import io.circe.generic.auto._
import org.zalando.grafter.{ Start, StartResult, Stop, StopResult }

final class OneForgeDataClient[F[_]](
    config: ForexConfig,
)(implicit F: Async[F])
    extends OneForgeDataClientAlg[F]
    with Start
    with Stop {

  private implicit lazy val sttpBackend =
    AsyncHttpClientCatsBackend(
      options = SttpBackendOptions(
        connectionTimeout = config.connectionTimeout,
        proxy = None,
      )
    )

  def marketOpen: F[Boolean] =
    for {
      response ← sttp
        .get(uri"https://forex.1forge.com/1.0.3/market_status?api_key=${config.apiKey}")
        .response(asJson[Json])
        .send()

      res ← response.body match {
        case Left(errorMsg)     ⇒ F.raiseError(Error.System(new RuntimeException(errorMsg)))
        case Right(Left(error)) ⇒ F.raiseError(Error.System(new RuntimeException(error.message)))
        case Right(Right(json)) ⇒
          F.pure(
            json.hcursor
              .get[Boolean]("market_is_open")
              .toOption
              .getOrElse(false)
          )
      }

    } yield res

  def quotes(request: List[Rate.Pair]): F[List[Rate]] = {
    val pairs = request.map(_.symbol).mkString(",")

    for {
      response ← sttp
        .get(uri"https://forex.1forge.com/1.0.3/quotes?pairs=$pairs&api_key=${config.apiKey}")
        .response(asJson[List[OneForgeResponse]])
        .send()

      res ← response.body match {
        case Left(errorMsg)     ⇒ F.raiseError(Error.System(new RuntimeException(errorMsg)))
        case Right(Left(error)) ⇒ F.raiseError(Error.System(new RuntimeException(error.message)))
        case Right(Right(quotes)) ⇒
          quotes
            .traverse(quote ⇒ F.delay(quote.toRate))
      }

    } yield res
  }

  def quotaExceeded: F[Boolean] =
    for {
      response ← sttp
        .get(uri"https://forex.1forge.com/1.0.3/quota?api_key=${config.apiKey}")
        .response(asJson[Json])
        .send()

      res ← response.body match {
        case Left(errorMsg)     ⇒ F.raiseError(Error.System(new RuntimeException(errorMsg)))
        case Right(Left(error)) ⇒ F.raiseError(Error.System(new RuntimeException(error.message)))
        case Right(Right(json)) ⇒
          F.pure(
            json.hcursor
              .get[Int]("quota_remaining")
              .toOption
              .contains(0)
          )
      }

    } yield res

  override def start: Eval[StartResult] =
    StartResult.eval("OneForgeDataClient") {}

  override def stop: Eval[StopResult] =
    StopResult.eval("OneForgeDataClient") {
      sttpBackend.close()
    }
}
