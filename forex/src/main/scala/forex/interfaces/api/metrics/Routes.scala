package forex.interfaces.api
package metrics

import akka.http.scaladsl._
import com.lonelyplanet.prometheus.api.MetricsEndpoint
import forex.config._
import io.prometheus.client.CollectorRegistry
import org.zalando.grafter.macros._

@readerOf[ApplicationConfig]
case class Routes(
) {

  lazy val metricsEndpoint = new MetricsEndpoint(CollectorRegistry.defaultRegistry)

  lazy val route: server.Route = metricsEndpoint.routes

}

object Routes {
}
