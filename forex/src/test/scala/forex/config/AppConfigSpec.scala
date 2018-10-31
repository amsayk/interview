package forex.config

import org.scalatest.{ FunSuite, Matchers }

class AppConfigSpec extends FunSuite with Matchers {

  test("load config") {

    pureconfig.loadConfig[ApplicationConfig]("app") shouldBe 'right
  }

}
