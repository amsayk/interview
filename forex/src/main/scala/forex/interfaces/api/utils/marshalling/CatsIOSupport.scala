package forex.interfaces.api.utils.marshalling

import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._

import cats.effect.IO

import scala.concurrent._

trait CatsIOSupport {

  implicit def ioMarshaller[T](
      implicit
      m: ToResponseMarshaller[Future[T]]
  ): ToResponseMarshaller[IO[T]] =
    Marshaller[IO[T], HttpResponse] { implicit ec ⇒ io ⇒
      val res = io.unsafeToFuture()
      m(res)
    }

}

object CatsIOSupport extends CatsIOSupport
