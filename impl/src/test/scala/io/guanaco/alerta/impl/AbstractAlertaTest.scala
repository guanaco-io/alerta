package io.guanaco.alerta.impl

import org.apache.activemq.camel.component.ActiveMQComponent
import org.apache.camel.CamelContext
import org.apache.camel.test.junit4.CamelTestSupport
import org.apache.commons.io.IOUtils

/**
  * Abstract test class for writing Alerta unit tests.
  */
abstract class AbstractAlertaTest extends CamelTestSupport {

  @throws[Exception]
  override protected def createCamelContext: CamelContext = {
    val context = super.createCamelContext
    context.addComponent("activemq", ActiveMQComponent.activeMQComponent("vm://test?broker.useJmx=false&broker.persistent=false"))
    context
  }
}

object AbstractAlertaTest {

  def readExpected(name: String): String =
    IOUtils.toString(getClass.getClassLoader.getResourceAsStream(s"expected/${name}")).trim
}