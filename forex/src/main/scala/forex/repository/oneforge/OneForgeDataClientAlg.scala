package forex.repository
package oneforge

import cats.data.Reader
import forex.config.ApplicationConfigReader
import forex.domain.Rate

import cats.effect.IO

trait OneForgeDataClientAlg[F[_]] {
  def marketOpen: F[Boolean]

  def quotes(request: List[Rate.Pair]): F[List[Rate]]

  def quotaExceeded: F[Boolean]
}

object OneForgeDataClientAlg {
  implicit val reader: ApplicationConfigReader[OneForgeDataClientAlg[IO]] = Reader(
    config ⇒ new OneForgeDataClient(config.forexConfig)
  )
}
