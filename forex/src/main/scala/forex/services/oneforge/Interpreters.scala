package forex.services
package oneforge

import java.util.concurrent.TimeUnit

import cats.data.EitherT
import cats.implicits._
import forex.domain._
import forex.repository.oneforge.OneForgeDataClientAlg
import cats.effect.{ Clock, Concurrent, ExitCase, Timer }
import forex.config.ForexConfig
import forex.repository.cache.CacheClientAlg
import fs2.concurrent.Queue
import scalacache.Mode
import java.time._

import scala.concurrent.duration.Duration

object Interpreters {
  def apply[F[_]: Mode: Concurrent: Timer](
      config: ForexConfig,
      client: OneForgeDataClientAlg[F],
      cache: CacheClientAlg[F],
  ): Algebra[F] = new Impl[F](config, client, cache)
}

final class Impl[F[_]: Mode: Timer] private[oneforge] (
    config: ForexConfig,
    client: OneForgeDataClientAlg[F],
    cache: CacheClientAlg[F],
)(implicit F: Concurrent[F], clock: Clock[F])
    extends Algebra[F] {

  def live(req: Rate.Pair): fs2.Stream[F, Error Either Rate] =
    fs2.Stream.eval(Queue.unbounded[F, Option[Error Either Rate]]).flatMap { queue ⇒
      val ticks = fs2.Stream.eval(clock.monotonic(TimeUnit.NANOSECONDS)) ++ fs2.Stream
        .awakeDelay[F](Duration(config.cacheTTL, TimeUnit.SECONDS))

      queue.dequeue.unNoneTerminate
        .filterWithPrevious { // send changes only
          case (Right(r1), Right(r2)) ⇒ r1.price != r2.price
          case _                      ⇒ true
        }
        .concurrently {

          ticks
            .evalMap(_ ⇒ get(req))
            .rethrow
            .evalMap(rate ⇒ queue.enqueue1(Some(Right(rate))))
            .drain
            .onFinalizeCase {
              case ExitCase.Error(exception) ⇒
                val error = exception match {
                  case e: Error     ⇒ e
                  case e: Throwable ⇒ Error.System(e)
                }

                queue.enqueue1(Some(Left(error))) >> queue.enqueue1(None)

              case _ ⇒ queue.enqueue1(None)
            }

        }

    }

  def get(req: Rate.Pair): F[Error Either Rate] = {
    val request = req :: Nil

    def fetchFromCache: F[Option[Rate.Cached]] = cache.get(req.symbol)

    def fetchFromNetwork: EitherT[F, Error, Rate] =
      for {
        quotes ← client.quotes(request)

        quote ← quotes.find(_.pair.symbol == req.symbol) match {
          case Some(v) ⇒ EitherT.pure[F, Error](v)
          case _       ⇒ EitherT.leftT[F, Rate](Error.RateNotFound(req): Error)
        }

        endOfDay = LocalDate.now.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault).toEpochSecond

        now ← EitherT.liftF(clock.monotonic(TimeUnit.SECONDS))

        ttl = Duration(
          endOfDay - now,
          TimeUnit.SECONDS,
        )

        _ ← EitherT.liftF(quotes.traverse { q ⇒
          cache.put(q.pair.symbol, Rate.Cached(q, now))(ttl)
        })

      } yield quote

    def isExpired(q: Rate.Cached, now: Long) = (now - q.time) > config.cacheTTL

    (for {
      now ← EitherT.liftF(clock.monotonic(TimeUnit.SECONDS))

      rate ← EitherT.liftF(fetchFromCache)

      r ← rate match {
        case Some(r) if !isExpired(r, now) ⇒ EitherT.pure[F, Error](r.rate)
        case _                             ⇒ fetchFromNetwork
      }

    } yield r).value
  }

}
