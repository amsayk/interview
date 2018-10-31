package forex.repository
package cache

import cats.data.Reader
import cats.effect.IO
import forex.config.ApplicationConfigReader
import forex.domain.Rate

import scala.concurrent.duration.Duration

trait CacheClientAlg[F[_]] {
  def get(symbol: String): F[Option[Rate.Cached]]

  def put(symbol: String, value: Rate.Cached)(ttl: Duration): F[Any]
}

object CacheClientAlg {
  import scalacache.CatsEffect.modes.io

  implicit val reader: ApplicationConfigReader[CacheClientAlg[IO]] =
    Reader(_ ⇒ new CacheClientCache2kCache)
}
