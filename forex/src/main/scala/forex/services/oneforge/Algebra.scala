package forex.services.oneforge

import forex.domain._

trait Algebra[F[_]] {
  def get(pair: Rate.Pair): F[Error Either Rate]

  def live(pair: Rate.Pair): fs2.Stream[F, Error Either Rate]
}
