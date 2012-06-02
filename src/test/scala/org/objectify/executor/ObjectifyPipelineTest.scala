package org.objectify.executor

import org.scalatest.{BeforeAndAfterEach, WordSpec}
import org.scalatest.mock.MockitoSugar
import org.objectify.HttpMethod._
import org.mockito.Mockito._
import org.objectify.policies.{AuthenticationPolicy, BadPolicy, GoodPolicy, Policy}
import org.objectify.services.PicturesIndexService
import org.objectify.responders.{PicturesIndexResponder, BadPolicyResponder}
import org.scalatest.matchers.ShouldMatchers
import org.objectify.adapters.ObjectifyRequestAdapter
import org.objectify.{ObjectifySugar, Action, Objectify}
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

/**
  * Testing the pipeline and sub-methods
  */
@RunWith(classOf[JUnitRunner])
class ObjectifyPipelineTest extends WordSpec with BeforeAndAfterEach with MockitoSugar with ObjectifySugar with ShouldMatchers {
    val objectify = Objectify()
    val pipeline = new ObjectifyPipeline(objectify)

    val req = mock[ObjectifyRequestAdapter]
    var action = Some(Action(Get, "index",
        policies = Some(Map(
            ~:[GoodPolicy] -> ~:[BadPolicyResponder],
            ~:[BadPolicy] -> ~:[BadPolicyResponder])
        ),
        service = Some(~:[PicturesIndexService]),
        responder = Some(~:[PicturesIndexResponder])))

    override protected def beforeEach() {
        objectify.defaults policy ~:[Policy]

        objectify.actions resource("pictures", index = action)

        // mock HTTP request methods
        when(req.getHttpMethod).thenReturn(Get)
        when(req.getPath).thenReturn("/pictures")
    }

    "The handle method" should {
        "execute policies fail" in {
            // do the method call
            val response = pipeline.handleRequest(action.get, req)

            // verify it worked
            response.getSerializedEntity should equal(new BadPolicyResponder()())
        }

        "execute policies pass with resolver" in {
            action = Some(Action(Get, "index",
                policies = Some(Map(
                    ~:[GoodPolicy] -> ~:[BadPolicyResponder],
                    ~:[AuthenticationPolicy] -> ~:[BadPolicyResponder])
                ),
                service = Some(~:[PicturesIndexService]),
                responder = Some(~:[PicturesIndexResponder]))
            )

            // do the method call
            val response = pipeline.handleRequest(action.get, req)

            // verify it worked
            response.getSerializedEntity should equal("index")
        }
    }
}
