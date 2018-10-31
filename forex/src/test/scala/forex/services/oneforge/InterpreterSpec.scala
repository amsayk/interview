package forex.services
package oneforge

import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

import cats.effect.{ ContextShift, IO, Timer }
import forex.config.ForexConfig
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.repository.cache.CacheClientAlg
import forex.repository.oneforge.OneForgeDataClientAlg
import org.scalatest._

import scala.concurrent.duration.Duration
import scalacache.CatsEffect.modes.io

class InterpreterSpec extends WordSpec with MustMatchers {
  implicit val ioTimer: Timer[IO] = IO.timer(scala.concurrent.ExecutionContext.global)
  implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  "get quote API" when {

    "market closed" should {

      "fail" in new Context {
        override val marketOpen: IO[Boolean] = IO.pure(false)

        OneForge.get(Rate.Pair(Currency.EUR, Currency.JPY)).unsafeRunSync() mustBe Left(OneForgeError.MarketClosed)

        a[OneForgeError.MarketClosed.type] should be thrownBy {
          OneForge.live(Rate.Pair(Currency.EUR, Currency.JPY)).take(1).compile.lastOrError.unsafeRunSync
        }

      }
    }

    "quota exceeded" should {

      "fail" in new Context {

        override val quotaExceeded: IO[Boolean] = IO.pure(true)

        OneForge.get(Rate.Pair(Currency.EUR, Currency.JPY)).unsafeRunSync() mustBe Left(OneForgeError.QuotaExceeded)

        a[OneForgeError.QuotaExceeded.type] should be thrownBy {
          OneForge.live(Rate.Pair(Currency.EUR, Currency.JPY)).compile.lastOrError.unsafeRunSync
        }

      }
    }

    "else" should {

      "get rate successfully" in new Context {
        val pair = Rate.Pair(Currency.EUR, Currency.JPY)

        val rate = Rate(
          pair = pair,
          price = Price(1.200),
          timestamp = Timestamp(OffsetDateTime.now),
        )

        override def quotes(request: List[Rate.Pair]): IO[List[Rate]] =
          IO.pure(
            List(
              rate
            )
          )

        OneForge.get(pair).unsafeRunSync() mustBe Right(rate)

        OneForge.live(pair).take(1).compile.lastOrError.unsafeRunSync mustBe Right(
          rate
        )
      }

      "visit cache" in new Context {
        val pair = Rate.Pair(Currency.EUR, Currency.JPY)

        val rate = Rate(
          pair = pair,
          price = Price(1.200),
          timestamp = Timestamp(OffsetDateTime.now),
        )

        override def quotes(request: List[Rate.Pair]): IO[List[Rate]] =
          IO.pure(
            List(
              rate
            )
          )

        var askedCache = false
        var rateCached = Option.empty[Rate]

        override def get(
            symbol: String
        ): IO[Option[Rate.Cached]] = IO.delay({ askedCache = true; None })

        override def put(symbol: String, value: Rate.Cached)(ttl: Duration): IO[Any] =
          IO.delay({ rateCached = Some(value.rate) })

        OneForge.get(pair).unsafeRunSync()

        askedCache mustBe true
        rateCached mustBe Some(rate)

        // Reset
        askedCache = false
        rateCached = Option.empty[Rate]

        OneForge.live(pair).take(1).compile.lastOrError.unsafeRunSync()

        askedCache mustBe true
        rateCached mustBe Some(rate)
      }
    }
  }

  class Context extends OneForgeDataClientAlg[IO] with CacheClientAlg[IO] {
    def marketOpen: IO[Boolean] = IO.pure(true)

    def quotes(request: List[Rate.Pair]): IO[List[Rate]] = IO.pure(List.empty)

    def quotaExceeded: IO[Boolean] = IO.pure(false)

    lazy val config = ForexConfig(
      apiKey = "_my api key_",
      cacheTTL = 5,
      connectionTimeout = Duration(30, TimeUnit.SECONDS),
    )

    def get(
        symbol: String
    ): IO[Option[Rate.Cached]] = IO.pure(None)

    def put(symbol: String, value: Rate.Cached)(ttl: Duration): IO[Any] = IO.pure(())

    lazy val OneForge = Interpreters[IO](config, this, this)
  }
}
