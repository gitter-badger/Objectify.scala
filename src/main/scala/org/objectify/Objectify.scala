package org.objectify

import adapters.{ObjectifyResponseAdapter, ObjectifyRequestAdapter}
import executor.{ObjectifyResponse, ObjectifyPipeline}
import com.twitter.logging.Logger
import resolvers.ClassResolver
import responders.ServiceResponder

/**
  * Main Objectify object for extending with your framework of choice
  */
case class Objectify(defaults: Defaults = Defaults(), actions: Actions = Actions())
    extends ObjectifySugar with ObjectifyImplicits {
    private val logger = Logger(classOf[Objectify])

    override def toString = {
        "Objectify Configuration" + actions.toString
    }

    def bootstrap() {
        actions.bootstrapValidation()
    }

    /**
      * Execute the request through the pipeline
      * @param action - the Action to execute
      * @param requestAdapter - the adapter to handle the request
      * @return - the response wrapped in an ObjectifyResponse
      */
    def execute(action: Action, requestAdapter: ObjectifyRequestAdapter): ObjectifyResponse[_] = {
        val pipeline = new ObjectifyPipeline(this)
        val start = System.currentTimeMillis()
        try {
            pipeline.handleRequest(action, requestAdapter)
        }
        finally {
            // want to get a logging statement even if request throws exception
            val requestTime = System.currentTimeMillis() - start
            logger.info("Request [%s - %s] took [%sms] for action [%s]."
                .format(requestAdapter.getHttpMethod, requestAdapter.getPath, requestTime, action))
        }
    }

    def locateResponseAdapter(response: ObjectifyResponse[_]): ObjectifyResponseAdapter[_] = {
        ClassResolver.resolveResponseAdapter(response.entity.getClass).newInstance()
    }

    var postServiceHook = (serviceResult: Any, responder: ServiceResponder[_, _]) => {}
}
