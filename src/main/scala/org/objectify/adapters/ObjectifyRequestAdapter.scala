/*
 * -------------------------------------------------------------------------------------------------
 * - Project:   Objectify                                                                          -
 * - Copyright: ©2014 Matygo Educational Incorporated operating as Learndot                        -
 * - Author:    Arthur Gonigberg (arthur@learndot.com) and contributors (see contributors.txt)     -
 * - License:   Licensed under MIT license (see license.txt)                                       -
 * -------------------------------------------------------------------------------------------------
 */

package org.objectify.adapters

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.apache.commons.fileupload.FileItem
import org.objectify.HttpMethod

/**
 * Adapter for requests
 */
trait ObjectifyRequestAdapter {
  def getPath: String

  def getQueryParameters: Map[String, List[String]]

  def getPathParameters: Map[String, String]

  def getHttpMethod: HttpMethod.Value

  def getBody: String

  def getFileParams: Map[String, FileItem]

  def getCookies: Map[String, String]

  def getResponse: HttpServletResponse

  def getRequest: HttpServletRequest

  def getHeader(string: String): Option[String]
}
