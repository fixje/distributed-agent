package org.mfuchs.distagent

import org.codehaus.jackson.map.annotate.JsonRootName

import scala.beans.BeanProperty

/**
  * More details about the service to provide to other services and instances
 *
  * @author Markus Fuchs (web-code@mfuchs.org)
  */
@JsonRootName("description")
class ServiceInstanceDetails(@BeanProperty description: String) {
  def this() { this("") }
}

