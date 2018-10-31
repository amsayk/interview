package forex.main

import forex.config._
import forex.repository.oneforge.OneForgeDataClientAlg
import forex.{ services ⇒ s }
import forex.{ processes ⇒ p }
import org.zalando.grafter.macros._
import cats.effect.{ ContextShift, IO, Timer }
import forex.repository.cache.CacheClientAlg

import scalacache.CatsEffect.modes.io

@readerOf[ApplicationConfig]
case class Processes(forexConfig: ForexConfig, client: OneForgeDataClientAlg[IO], cache: CacheClientAlg[IO]) {

  implicit val ioTimer: Timer[IO] = IO.timer(scala.concurrent.ExecutionContext.global)
  implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  implicit final lazy val _oneForge: s.OneForge[IO] =
    s.OneForge(forexConfig, client, cache)

  final val Rates = p.Rates[IO]

}

object Processes {}
