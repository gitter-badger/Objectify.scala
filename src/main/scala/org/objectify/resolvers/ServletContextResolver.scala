/*
 * -------------------------------------------------------------------------------------------------
 *  - Project:   Objectify                                                                           -
 *  - Copyright: ©2013 Matygo Educational Incorporated operating as Learndot                         -
 *  - Author:    Arthur Gonigberg (arthur@learndot.com) and contributors (see contributors.txt)      -
 *  - License:   Licensed under MIT license (see license.txt)                                         -
 *  -------------------------------------------------------------------------------------------------
 */

package org.objectify.resolvers

import javax.servlet.ServletContext
import org.objectify.adapters.ObjectifyRequestAdapter
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

/**
  * Resolver for the servlet context
  */
class ServletContextResolver extends Resolver[ServletContext, ObjectifyRequestAdapter] {
    def apply(req: ObjectifyRequestAdapter): ServletContext = {
        req.getRequest.getServletContext
    }
}




