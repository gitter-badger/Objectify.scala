/*
 * -------------------------------------------------------------------------------------------------
 * - Project:   Objectify                                                                          -
 * - Copyright: ©2014 Matygo Educational Incorporated operating as Learndot                        -
 * - Author:    Arthur Gonigberg (arthur@learndot.com) and contributors (see contributors.txt)     -
 * - License:   Licensed under MIT license (see license.txt)                                       -
 * -------------------------------------------------------------------------------------------------
 */

package org.objectify.adapters

import org.objectify.executor.ObjectifyResponse
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.objectify.exceptions.ConfigurationException

/**
  * Response adapters are an easy way to allow for serializing any type you desire
  */
trait ObjectifyResponseAdapter[T] {
    def serializeResponseAny(request: HttpServletRequest, response: HttpServletResponse,
                          objectifyResponse: ObjectifyResponse[_]) {
        val castResponse = if (objectifyResponse != null && objectifyResponse.isInstanceOf[T]) {
            objectifyResponse.asInstanceOf[ObjectifyResponse[T]]
        }
        else {
            throw new ConfigurationException("The response and response adapter provided are not compatible.")
        }

        serializeResponse(request, response, castResponse)
    }

    def serializeResponse(request: HttpServletRequest, response: HttpServletResponse,
                          objectifyResponse: ObjectifyResponse[T])
}
