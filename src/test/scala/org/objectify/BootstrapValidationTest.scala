package org.objectify

import adapters.ObjectifyScalatraAdapter
import exceptions.ConfigurationException
import org.scalatest.{BeforeAndAfterEach, WordSpec}
import org.scalatest.matchers.ShouldMatchers
import org.scalatra.ScalatraFilter
import org.scalatra.test.scalatest.ScalatraSuite
import responders.ServiceResponder
import services.PicturesIndexService
import org.scalatest.mock.MockitoSugar
import HttpMethod._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
  * Making sure bootstrap validation works correctly
  */
@RunWith(classOf[JUnitRunner])
class BootstrapValidationTest
    extends WordSpec with ShouldMatchers with BeforeAndAfterEach with ScalatraSuite with ObjectifySugar with MockitoSugar {

    val scalatrafied = new ObjectifyScalatraAdapter with ScalatraFilter {
        get("/test") {
            "win"
        }
    }

    override def beforeEach() {
        addFilter(scalatrafied, "/*")
    }

    "Bootstrap validation" should {
        "pass the control test" in {
            scalatrafied.actions resource ("pictures")
            scalatrafied.bootstrap()

            get("/test") {
                status should be(200)
                body should be("win")
            }
        }
        "Fail when a service doesn't exist" in {
            scalatrafied.actions action(Get, "asdf", "asdf",
                policies = None,
                service = None,
                responder = None
            )
            val thrown = evaluating { scalatrafied.bootstrap() } should produce [ConfigurationException]
            thrown.getMessage should equal("No class matching the name: AsdfService")
        }
        "Fail when responders don't exist" in {
            scalatrafied.actions action(Get, "asdf", "asdf",
                policies = None,
                service = -:[PicturesIndexService],
                responder = None
            )
            val thrown = evaluating { scalatrafied.bootstrap() } should produce [ConfigurationException]
            thrown.getMessage should equal("No class matching the name: AsdfResponder")
        }
        "Fail when responders don't match up with services" in {
            scalatrafied.actions action(Get, "asdf", "asdf",
                policies = None,
                service = -:[PicturesIndexService],
                responder = -:[NonStringResponder]
            )
            val thrown = evaluating { scalatrafied.bootstrap() } should produce [ConfigurationException]
            thrown.getMessage should equal("Service [class org.objectify.services.PicturesIndexService] and " +
                "Responder [class org.objectify.NonStringResponder] are not compatible. Service return " +
                "type [class java.lang.String] does not match Responder apply method parameter [public java.lang.String org.objectify.NonStringResponder.apply(int)].")
        }
    }
}

class NonStringResponder extends ServiceResponder[String, Int] {
    def apply(serviceResult: Int) = serviceResult.toString
}