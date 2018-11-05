package forex.repository
package oneforge

import cats.data.{ EitherT, Reader }
import forex.config.ApplicationConfigReader
import forex.domain.Rate
import cats.effect.IO
import forex.services.oneforge.Error

trait OneForgeDataClientAlg[F[_]] {
  def quotes(request: List[Rate.Pair]): EitherT[F, Error, List[Rate]]
}

object OneForgeDataClientAlg {
  implicit val reader: ApplicationConfigReader[OneForgeDataClientAlg[IO]] = Reader(
    config ⇒ new OneForgeDataClient(config.forexConfig)
  )
}
