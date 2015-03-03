/*
 * -------------------------------------------------------------------------------------------------
 * - Project:   Objectify                                                                          -
 * - Copyright: ©2014 Matygo Educational Incorporated operating as Learndot                        -
 * - Author:    Arthur Gonigberg (arthur@learndot.com) and contributors (see contributors.txt)     -
 * - License:   Licensed under MIT license (see license.txt)                                       -
 * -------------------------------------------------------------------------------------------------
 */

package org.objectify.services

import org.objectify.exceptions.{BadRequestException, ConfigurationException, ObjectifyException}


/**
 * This simulates a broken service
 */
class ExceptionThrowingServices {}

class ThrowsBadRequest extends Service[String] {
  def apply(): String = throw new BadRequestException("somethingawful")
}

class ThrowsUnexpected extends Service[String] {
  def apply(): String = throw new NullPointerException("whoa whoa whoa... hold up man... whoa")
}

class ThrowsConfig extends Service[String] {
  def apply(): String = throw new ConfigurationException("somethingawful")
}

class Throws403 extends Service[String] {
  def apply(): String = throw new ObjectifyException(403, "somethingawful")
}

class NullService extends Service[String] {
  def apply(): String = "null"
}

