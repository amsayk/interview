package forex.repository
package cache

import java.util.concurrent.TimeUnit

import cats.Eval
import forex.domain.Rate
import org.cache2k.Cache2kBuilder
import org.zalando.grafter.{ Start, StartResult, Stop, StopResult }
import scalacache.cache2k.Cache2kCache
import scalacache.{ Cache, Mode }

import scala.concurrent.duration.Duration

final class CacheClientCache2kCache[F[_]: Mode: cats.effect.Sync] extends CacheClientAlg[F] with Start with Stop {

  lazy val cache2kCache: Cache[Rate.Cached] = Cache2kCache(
    new Cache2kBuilder[String, Rate.Cached]() {}
      .name("forex")
      .expireAfterWrite(60 * 24, TimeUnit.MINUTES)
      .build
  )

  def get(symbol: String): F[Option[Rate.Cached]] = cache2kCache.get(symbol)

  def put(symbol: String, value: Rate.Cached)(ttl: Duration): F[Any] =
    cache2kCache.put(symbol)(value, ttl = Some(ttl))

  override def start: Eval[StartResult] =
    StartResult.eval("Cache2kCache") {}

  override def stop: Eval[StopResult] =
    StopResult.eval("Cache2kCache") {
      cache2kCache.close()
    }
}
