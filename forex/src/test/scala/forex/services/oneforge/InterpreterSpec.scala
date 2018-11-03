package forex.services
package oneforge

import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

import cats.effect.{ Clock, ContextShift, IO, Timer }
import forex.config.ForexConfig
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.repository.cache.CacheClientAlg
import forex.repository.oneforge.OneForgeDataClientAlg
import org.scalatest._

import scala.concurrent.duration.{ Duration, FiniteDuration, NANOSECONDS, SECONDS, TimeUnit }
import scalacache.CatsEffect.modes.io

class InterpreterSpec extends WordSpec with MustMatchers {
  val ioTimer: Timer[IO] = IO.timer(scala.concurrent.ExecutionContext.global)
  implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  "get quote API" when {

    "market closed" should {

      "fail" in new Context {
        override val marketOpen: IO[Boolean] = IO.pure(false)

        OneForge.get(Rate.Pair(Currency.EUR, Currency.JPY)).unsafeRunSync() mustBe Left(OneForgeError.MarketClosed)

        a[OneForgeError.MarketClosed.type] should be thrownBy {
          OneForge.live(Rate.Pair(Currency.EUR, Currency.JPY)).compile.lastOrError.unsafeRunSync()
        }

      }
    }

    "quota exceeded" should {

      "fail" in new Context {

        override val quotaExceeded: IO[Boolean] = IO.pure(true)

        OneForge.get(Rate.Pair(Currency.EUR, Currency.JPY)).unsafeRunSync() mustBe Left(OneForgeError.QuotaExceeded)

        a[OneForgeError.QuotaExceeded.type] should be thrownBy {
          OneForge.live(Rate.Pair(Currency.EUR, Currency.JPY)).compile.lastOrError.unsafeRunSync()
        }

      }
    }

    "market open and quota available" should {

      "get rate successfully" in new Context {
        val pair = Rate.Pair(Currency.EUR, Currency.JPY)

        val rate = Rate(
          pair = pair,
          price = Price(1.20),
          timestamp = Timestamp(OffsetDateTime.now),
        )

        override def quotes(request: List[Rate.Pair]): IO[List[Rate]] =
          IO.pure(
            List(
              rate
            )
          )

        OneForge.get(pair).unsafeRunSync() mustBe Right(rate)

        OneForge.live(pair).take(1).compile.lastOrError.unsafeRunSync() mustBe Right(
          rate
        )
      }

      "visit cache" in new Context {
        val pair = Rate.Pair(Currency.EUR, Currency.JPY)

        val rate = Rate(
          pair = pair,
          price = Price(1.20),
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

    "cache" should {

      "miss if cacheTTL expires" in new Context {
        val pair = Rate.Pair(Currency.EUR, Currency.JPY)

        val rate = Rate(
          pair = pair,
          price = Price(1.20),
          timestamp = Timestamp(OffsetDateTime.now),
        )

        val now = System.nanoTime

        override def clock: Clock[IO] = new Clock[IO] {
          override def realTime(unit: TimeUnit): IO[Long] = ???

          override def monotonic(unit: TimeUnit): IO[Long] =
            IO.delay(unit.convert(now, NANOSECONDS))
        }

        // make it expired
        val cacheTime = SECONDS.convert(now, NANOSECONDS) - (config.cacheTTL + 1)

        var hit = false
        var putCalled = false

        override def quotes(request: List[Rate.Pair]): IO[List[Rate]] =
          IO.delay({ hit = true; List(rate) })

        override def get(
            symbol: String
        ): IO[Option[Rate.Cached]] = IO.pure(Some(Rate.Cached(rate, cacheTime)))

        override def put(symbol: String, value: Rate.Cached)(ttl: Duration): IO[Any] = IO.delay({ putCalled = true })

        OneForge.get(pair).unsafeRunSync()

        hit mustBe true
        putCalled mustBe true

      }

      "hit if cacheTTL did not expire" in new Context {
        val pair = Rate.Pair(Currency.EUR, Currency.JPY)

        val rate = Rate(
          pair = pair,
          price = Price(1.20),
          timestamp = Timestamp(OffsetDateTime.now),
        )

        val now = System.nanoTime

        override def clock: Clock[IO] = new Clock[IO] {
          override def realTime(unit: TimeUnit): IO[Long] = ???

          override def monotonic(unit: TimeUnit): IO[Long] =
            IO.delay(unit.convert(now, NANOSECONDS))
        }

        // not quite expired
        val cacheTime = SECONDS.convert(now, NANOSECONDS) - config.cacheTTL

        var hit = false
        var putCalled = false

        override def quotes(request: List[Rate.Pair]): IO[List[Rate]] =
          IO.delay({ hit = true; List(rate) })

        override def get(
            symbol: String
        ): IO[Option[Rate.Cached]] = IO.pure(Some(Rate.Cached(rate, cacheTime)))

        override def put(symbol: String, value: Rate.Cached)(ttl: Duration): IO[Any] = IO.delay({ putCalled = true })

        OneForge.get(pair).unsafeRunSync()

        hit mustBe false
        putCalled mustBe false
      }

    }

  }

  class Context extends OneForgeDataClientAlg[IO] with CacheClientAlg[IO] { self ⇒
    def marketOpen: IO[Boolean] = IO.pure(true)

    def quotes(request: List[Rate.Pair]): IO[List[Rate]] = IO.pure(List.empty)

    def quotaExceeded: IO[Boolean] = IO.pure(false)

    def cacheTTL = 5L

    def config = ForexConfig(
      apiKey = "_my api key_",
      cacheTTL = cacheTTL,
      connectionTimeout = Duration(30, TimeUnit.SECONDS),
    )

    def get(
        symbol: String
    ): IO[Option[Rate.Cached]] = IO.pure(None)

    def put(symbol: String, value: Rate.Cached)(ttl: Duration): IO[Any] = IO.pure(())

    def clock = Clock.create[IO]

    implicit def timer = new Timer[IO] {
      def clock: Clock[IO] = self.clock
      def sleep(duration: FiniteDuration): IO[Unit] = ioTimer.sleep(duration)
    }

    lazy val OneForge = Interpreters[IO](config, this, this)
  }
}
