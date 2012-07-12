package org.objectify

import exceptions.ConfigurationException
import org.objectify.policies.Policy
import org.objectify.services.Service
import mojolly.inflector.InflectorImports._
import resolvers.ClassResolver
import responders.{PolicyResponder, ServiceResponder}

object HttpMethod extends Enumeration {
    type HttpMethod = Value
    val Head, Get, Post, Put, Delete, Trace, Options, Connect, Patch = Value
}

object ContentType extends Enumeration {
    type ContentType = Value
    val JSON, HTML, XML, TEXT = Value

    def getTypeString(contentType: ContentType) = contentType match {
        case JSON => "application/json"
        case XML => "application/xml"
        case HTML => "text/html"
        case _ => "text/plain"
    }
}

import HttpMethod._
import ContentType._

/**
  * An Objectify Action is a mapping of an HTTP Verb + URL pattern to a
  * set of policies a service and a responder
  */
case class Action(method: HttpMethod,
                  var name: String,
                  contentType: ContentType = JSON,
                  var route: Option[String] = None,
                  var policies: Option[Map[Class[_ <: Policy], Class[_ <: PolicyResponder[_]]]] = None,
                  service: Option[Class[_ <: Service[_]]] = None,
                  responder: Option[Class[_ <: ServiceResponder[_, _]]] = None,
                  var ignoreGlobalPolicies: Boolean = false) {

    // conditionally set the route
    def setRouteIfNone(newRoute: String) {
        if (route.isEmpty) {
            route = Some(newRoute)
        }
    }

    def resolvePolicies: Map[Class[_ <: Policy], Class[_ <: PolicyResponder[_]]] = {
        policies.getOrElse(Map())
    }

    /**
      * Resolves the service class by either returning the preset service class
      * or find the class based on the name of this action
      *
      * eg: pictures index => PicturesDeleteService
      * @return
      */
    def resolveServiceClass: Class[_ <: Service[_]] = {
        service.getOrElse(ClassResolver.resolveServiceClass(getSerivceClassName(name)))
    }

    def resolveResponderClass: Class[_ <: ServiceResponder[_, _]] = {
        responder.getOrElse(ClassResolver.resolveResponderClass(getResponderClassName(name)))
    }

    private def getSerivceClassName(name: String) = {
        name + "Service"
    }

    private def getResponderClassName(name: String) = {
        name + "Responder"
    }

    override def toString = {
        val stringBuilder = new StringBuilder()
        stringBuilder.append("Actions(")
        stringBuilder.append(method)
        stringBuilder.append(',')
        stringBuilder.append(name)
        stringBuilder.append(',')
        stringBuilder.append(route.getOrElse("NO ROUTE"))
        stringBuilder.append(',')
        stringBuilder.append(resolveServiceClass)
        stringBuilder.append(")")
        stringBuilder.toString()
    }

    override def equals(other: Any): Boolean = {
        if (!other.isInstanceOf[Action]) {
            return false
        }
        val otherAction = other.asInstanceOf[Action]
        this.method == otherAction.method && this.name.equals(otherAction.name)
    }

    override def hashCode() = method.hashCode() + name.hashCode
}

class PolicyTuple(val tuple: (Class[_ <: Policy], Class[_ <: PolicyResponder[_]])) {
    var onlyStr: List[String] = Nil
    var exceptStr: List[String] = Nil

    def only(actions: String*) = {
        onlyStr = actions.toList
        this
    }

    def except(actions: String*) = {
        exceptStr = actions.toList
        this
    }
}

case class Actions() extends Iterable[Action] {

    var actions: Map[HttpMethod, Map[String, Action]] = HttpMethod.values.map(_ -> Map[String, Action]()).toMap

    def action(httpMethod: HttpMethod, name: String, route: String, contentType: ContentType = JSON,
               policies: Option[Map[Class[_ <: Policy], Class[_ <: PolicyResponder[_]]]] = None,
               service: Option[Class[_ <: Service[_]]] = None, responder: Option[Class[_ <: ServiceResponder[_, _]]] = None) {
        val action = Action(httpMethod, name, contentType, Some(route), policies, service, responder)

        resolveRouteAndName(Some(action), "", route)
    }

    /**
      * Default routing configuration point assumes to create an
      * policy free (public) set of routes that map to the
      * following services
      *
      * GET 	/#{name} 		#{name}IndexService
      * GET 	/#{name}/:id 	#{name}ShowService
      * GET 	/#{name}/new 	#{name}NewService
      * POST 	/#{name} 		#{name}CreationService
      * GET 	/#{name}/edit 	#{name}EditService
      * PUT 	/#{name}/:id	#{name}UpdateService
      * DELETE 	/#{name}/:id 	#{name}DestructionService
      */

    def resource(name: String,
                 index: Option[Action] = Some(Action(Get, "index")),
                 show: Option[Action] = Some(Action(Get, "show")),
                 `new`: Option[Action] = Some(Action(Get, "new")),
                 create: Option[Action] = Some(Action(Post, "create")),
                 edit: Option[Action] = Some(Action(Get, "edit")),
                 update: Option[Action] = Some(Action(Put, "update")),
                 destroy: Option[Action] = Some(Action(Delete, "destroy"))): Resource = {

        // update the routes if they haven't been set
        val route = name.pluralize

        // Ensure that all the actions have resty routes.
        resolveRouteAndName(index, route, route)
        resolveRouteAndName(show, route, route + "/:id")
        resolveRouteAndName(`new`, route, route + "/new")
        resolveRouteAndName(create, route, route)
        resolveRouteAndName(edit, route, route + "/:id/edit")
        resolveRouteAndName(update, route, route + "/:id")
        resolveRouteAndName(destroy, route, route + "/:id")

        new Resource(List(index, show, `new`, create, edit, update, destroy))
    }

    /**
      * This class is mainly here to help in creating a pretty syntax with chained calls
      */
    class Resource(private val actions: List[Option[Action]]) {
        def policy(policy: PolicyTuple): Resource = {
            val applyActions = getActionsFromPolicyTuple(policy)
            applyPolicies(applyActions, policy.tuple)
            this
        }

        def policies(policies: PolicyTuple*): Resource = {
            for (policy <- policies) {
                val applyActions = getActionsFromPolicyTuple(policy)
                applyPolicies(applyActions, policy.tuple)
            }
            this
        }

        private def getActionsFromPolicyTuple(tuple: PolicyTuple): List[Option[Action]] = {
            // either only or except -- not both
            if (tuple.onlyStr.nonEmpty) {
                string2Actions(tuple.onlyStr)
            }
            else if (tuple.exceptStr.nonEmpty) {
                actions.filterNot(string2Actions(tuple.exceptStr).contains(_))
            }
            else {
                actions
            }
        }

        // try to match up name with reverse of resolveRouteAndName
        private def string2Actions(actionStr: Seq[String]) = {
            actions.filter(action => {
                actionStr.filter(s => action.get.name.toLowerCase.endsWith(s)).size > 0
            })
        }

        // apply a single policy to any number of actions
        private def applyPolicies(actions: List[Option[Action]], policy: (Class[_ <: Policy], Class[_ <: PolicyResponder[_]])) {
            actions.flatten.foreach(action => {
                val actionPols = action.policies
                val allPolicies = if (actionPols.isDefined) actionPols.get ++ Map(policy) else Map(policy)
                action.policies = Some(allPolicies)
            })
        }
    }

    private def resolveRouteAndName(actionOption: Option[Action], namePrefix: String, route: String) {
        actionOption.map(a => {
            a.name = namePrefix.capitalize + a.name.capitalize
            a.setRouteIfNone(route)
            action(a)
        })
    }

    private def action(action: Action) {
        var map = actions(action.method)
        map += (action.route.get -> action)
        actions += (action.method -> map)
        action
    }

    override def iterator = {
        actions.values.flatMap(_.values).iterator
    }

    def bootstrapValidation() {
        for {
            (verb, actionsEntry) <- actions
            (string, action) <- actionsEntry
        } {
            action.resolvePolicies
            val service = action.resolveServiceClass
            val responder = action.resolveResponderClass

            // make sure service and resolver are compatible
            val returnType = service.getMethod("apply").getReturnType
            try {
                responder.getMethod("apply", returnType).getParameterTypes.head
            }
            catch {
                case e: NoSuchMethodException =>
                    val parameterType = responder.getMethods.filter(_.getName.startsWith("apply")).headOption
                    throw new ConfigurationException("Service [%s] and Responder [%s] are not compatible. " +
                        "Service return type [%s] does not match Responder apply method parameter [%s]."
                        format(service.toString, responder.toString, returnType.toString, parameterType.getOrElse("Undefined").toString))
            }
        }
    }

    override def toString() = {
        val stringBuilder = new StringBuilder()
        stringBuilder.append("\n")
        stringBuilder.append("Actions[")
        actions.foreach {
            case (verb, actions) => {
                stringBuilder.append("\n\t")
                stringBuilder.append(verb)
                actions.foreach {
                    case (route, action) => {
                        stringBuilder.append("\n\t\t")
                        stringBuilder.append(action)
                    }
                }
            }
        }
        stringBuilder.append("\n")
        stringBuilder.append("]")
        stringBuilder.toString
    }
}