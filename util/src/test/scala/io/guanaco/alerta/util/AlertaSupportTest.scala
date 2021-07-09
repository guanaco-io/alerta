package io.guanaco.alerta.util

import io.guanaco.alerta.api.{Alert, Alerta}
import io.guanaco.alerta.api.Alerta
import io.guanaco.alerta.impl.AlertaImpl
import io.guanaco.alerta.test.AlertaCamelTestSupport
import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.activemq.ActiveMQComponent
import org.apache.camel.test.junit4.CamelTestSupport
import org.junit.Assert._
import org.junit.Test

import scala.collection.JavaConverters._

/**
  * Created by gertv on 4/26/17.
  */
class AlertaSupportTest extends CamelTestSupport with AlertaCamelTestSupport {

  import AlertaSupportTest._

  @Test
  def testAlertaSupportSuccess(): Unit = {
    val alerts = getMockEndpoint(MOCK_ALERTS)
    alerts.expectedMessageCount(1)

    val service = new BusinessServices(new AlertaImpl(context()))
    service.succeed(INPUT)

    assertMockEndpointsSatisfied()

    val alert = alerts.getExchanges.asScala.head.getIn.getBody(classOf[Alert])
    assertEquals(s"resource.for.${INPUT}", alert.resource)
    assertEquals("BusinessServiceSuccess", alert.event)
    assertTrue(alert.correlate.get.contains("BusinessServiceFailure"))
    assertEquals(Map("static" -> "attr"), alert.attributes)
    assertEquals(Some("Marketing"), alert.customer)
  }

  @Test
  def testAlertaSupportFailure(): Unit = {
    val alerts = getMockEndpoint(MOCK_ALERTS)
    alerts.expectedMessageCount(1)

    val service = new BusinessServices(new AlertaImpl(context()))
    service.failed(INPUT)

    assertMockEndpointsSatisfied()

    val alert = alerts.getExchanges.asScala.head.getIn.getBody(classOf[Alert])
    assertEquals(s"resource.for.${INPUT}", alert.resource)
    assertEquals("BusinessServiceFailure", alert.event)
    assertTrue(alert.correlate.get.contains("BusinessServiceSuccess"))
    assertEquals(Map("static" -> "attr", "retry.path" -> "/test"), alert.attributes)
  }

  override def createRouteBuilder(): RouteBuilder = createAlertaRouteBuilder()

  override def createCamelContext(): CamelContext = {
    val context = super.createCamelContext()
    context.addComponent("activemq", ActiveMQComponent.activeMQComponent("vm://test?broker.persistent=false&broker.useJmx=false"))
    context
  }

  class BusinessServices(val alerta: Alerta) extends AlertaSupport {

    implicit val config = AlertaConfig("BusinessService", Seq("test", "service"), Map("static" -> "attr"), Option("Marketing")) { body: String =>
      s"resource.for.${body}"
    }

    def succeed(input: String) = {
      // do something right
      sendAlertaSuccess(input)
    }

    def failed(input: String) =
      try {
        // do something wrong
        throw new RuntimeException("Mislukt")
      } catch {
        case e: Exception => sendAlertaFailure(input, e, Map("retry.path" -> "/test"))
      }

  }

}

object AlertaSupportTest {

  val MOCK_ALERTS = "mock:alerts"
  val INPUT       = "MyImportantMethodInput"

}
