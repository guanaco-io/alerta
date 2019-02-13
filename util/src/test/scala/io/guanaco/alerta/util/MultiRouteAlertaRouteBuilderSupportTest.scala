package io.guanaco.alerta.util

import io.guanaco.alerta.api.Alert
import io.guanaco.alerta.impl.AlertaImpl
import io.guanaco.alerta.test.AlertaCamelTestSupport
import org.apache.activemq.camel.component.ActiveMQComponent
import org.apache.camel.{CamelContext, RoutesBuilder}
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.JndiRegistry
import org.apache.camel.test.junit4.CamelTestSupport
import org.junit.Assert._
import org.junit.Test

import scala.collection.JavaConversions._

/**
  * Test cases for [[AlertaRouteBuilderSupport]] using the implicit Scala DSL to apply Alerta config to a single route
  */
class MultiRouteAlertaRouteBuilderSupportTest extends CamelTestSupport with AlertaCamelTestSupport {

  import AlertaRouteBuilderSupportTest._
  import AlertaCamelTestSupport._

  @Test
  def testSuccess(): Unit = {
    val alerts = getMockEndpoint(MOCK_ALERTS)
    alerts.expectedMessageCount(1)
    getMockEndpoint(END).expectedMessageCount(1)

    template.sendBody(START, "working")

    assertMockEndpointsSatisfied()

    val alert = alerts.getExchanges.head.getIn.getBody(classOf[Alert])
    assertCorrelatedEvents(alert)
    assertEquals(s"${AlertaFlowId}Success", alert.event)
    assertEquals(s"${AlertaFlowId}:working", alert.resource)
    assertEquals("normal", alert.severity)
  }

  def assertCorrelatedEvents(alert: Alert): Unit = {
    val correlated = alert.correlate.get
    assertEquals(3, correlated.size)
    assertTrue(correlated.exists(_.endsWith("Success")))
    assertTrue(correlated.exists(_.endsWith("Warning")))
    assertTrue(correlated.exists(_.endsWith("Failure")))
  }

  override def createRouteBuilders(): Array[RoutesBuilder] = Array(
    createAlertaRouteBuilder(),
    new RouteBuilder() with AlertaRouteBuilderSupport {
      implicit val config = AlertaConfig(AlertaFlowId, Seq("service")) { value: String =>
        s"${AlertaFlowId}:${value}"
      }

      override def configure(): Unit = {

        //format: OFF
        from(START)
          .configureAlerta[String]
          .to("direct:subroute")

        from("direct:subroute")
          .to(END)
        //format: ON
      }
    })

  override def createCamelContext(): CamelContext = {
    val context = super.createCamelContext()
    context.addComponent("activemq", ActiveMQComponent.activeMQComponent("vm://test?broker.persistent=false&broker.useJmx=false"))
    context.getRegistry(classOf[JndiRegistry]).bind("alerta", new AlertaImpl(context))
    context
  }
}

object MultiRouteAlertaRouteBuilderSupportTest {

  val START = "direct:start"

  val END = "mock:end"
  val FAILED = "mock:failed"
  val HANDLED = "mock:handled"

  val MOCK_ALERTS = "mock:alerts"

  val AlertaFlowId = "MyAlertaFlowId"

}
