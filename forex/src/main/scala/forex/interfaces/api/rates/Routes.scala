package forex.interfaces.api.rates

import akka.http.scaladsl._
import akka.stream.scaladsl.{ Flow, Sink, Source }
import forex.config._
import forex.domain.Rate
import forex.main._
import forex.interfaces.api.utils._
import org.zalando.grafter.macros._
import fs2.interop.reactivestreams._
import cats.effect.IO
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import forex.processes.RatesError
import forex.processes.rates.messages.GetRequest
import io.circe.syntax._
import io.circe.generic.auto._

@readerOf[ApplicationConfig]
case class Routes(
    processes: Processes,
) {

  import server.Directives._
  import Directives._
  import Converters._
  import ApiMarshallers._

  import processes._

  lazy val route: server.Route =
    pathSingleSlash {
      get {
        getApiRequest { req ⇒
          complete {
            Rates
              .get(toGetRequest(req))
              .map(_.map(result ⇒ toGetApiResponse(result)))
          }
        }
      }
    } ~ pathPrefix("live") {
      getApiRequest { req ⇒
        handleWebSocketMessages(liveFlow(toGetRequest(req)))
      }
    }

  private def liveFlow(req: GetRequest): Flow[Message, Message, Any] = {
    val stream = Rates.live(req)

    val publisher: StreamUnicastPublisher[IO, RatesError Either Rate] = stream.toUnicastPublisher

    val source = Source
      .fromPublisher(publisher)
      .map {
        case Left(error) ⇒ TextMessage(toErrorApiResponse(error).asJson.noSpaces)
        case Right(rate) ⇒ TextMessage(rate.asJson.noSpaces)
      }

    Flow.fromSinkAndSource[Message, Message](Sink.ignore, source)
  }

}
