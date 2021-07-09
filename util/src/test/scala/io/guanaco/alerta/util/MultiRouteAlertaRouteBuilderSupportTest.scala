package io.guanaco.alerta.util

import io.guanaco.alerta.api.Alert
import io.guanaco.alerta.impl.AlertaImpl
import io.guanaco.alerta.test.AlertaCamelTestSupport
import org.apache.camel.{CamelContext, RoutesBuilder}
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.activemq.ActiveMQComponent
import org.apache.camel.test.junit4.CamelTestSupport
import org.junit.Assert._
import org.junit.Test

/**
  * Test cases for [[AlertaRouteBuilderSupport]] using the implicit Scala DSL to apply Alerta config to a single route
  */
class MultiRouteAlertaRouteBuilderSupportTest extends CamelTestSupport with AlertaCamelTestSupport {

  import MultiRouteAlertaRouteBuilderSupportTest._
  import scala.collection.JavaConverters._

  @Test
  def testSuccess(): Unit = {
    val alerts = getMockEndpoint(MOCK_ALERTS)
    alerts.expectedMessageCount(1)
    getMockEndpoint(END).expectedMessageCount(1)

    template.sendBody(START, "working")

    assertMockEndpointsSatisfied()

    val alert = alerts.getExchanges.asScala.head.getIn.getBody(classOf[Alert])
    assertCorrelatedEvents(alert)
    assertEquals(s"${AlertaFlowId}Success", alert.event)
    assertEquals(s"${AlertaFlowId}:working", alert.resource)
    assertEquals("normal", alert.severity)
  }

  @Test
  def testFailure(): Unit = {
    val alerts = getMockEndpoint(MOCK_ALERTS)
    alerts.expectedMessageCount(1)

    try {
      template.sendBody(FAIL, "working")
    } catch {
      case e: Exception => //graciously ignore this
    }

    assertMockEndpointsSatisfied()

    val alert = alerts.getExchanges.asScala.head.getIn.getBody(classOf[Alert])
    assertCorrelatedEvents(alert)
    assertEquals(s"${AlertaFlowId}Failure", alert.event)
    assertEquals(s"${AlertaFlowId}:working", alert.resource)
    assertEquals("minor", alert.severity)
  }

  def assertCorrelatedEvents(alert: Alert): Unit = {
    val correlated = alert.correlate.get
    assertEquals(3, correlated.size)
    assertTrue(correlated.exists(_.endsWith("Success")))
    assertTrue(correlated.exists(_.endsWith("Warning")))
    assertTrue(correlated.exists(_.endsWith("Failure")))
  }

  override def createRouteBuilders(): Array[RoutesBuilder] =
    Array(
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

          from("direct:fail")
            .configureAlerta[String]
            .throwException(new RuntimeException("This is broken!"))
          //format: ON
        }
      }
    )

  override def createCamelContext(): CamelContext = {
    val context = super.createCamelContext()
    context.addComponent("activemq", ActiveMQComponent.activeMQComponent("vm://test?broker.persistent=false&broker.useJmx=false"))
    context.getRegistry().bind("alerta", new AlertaImpl(context))
    context
  }
}

object MultiRouteAlertaRouteBuilderSupportTest {

  val START = "direct:start"
  val FAIL  = "direct:fail"

  val END     = "mock:end"
  val FAILED  = "mock:failed"
  val HANDLED = "mock:handled"

  val MOCK_ALERTS = "mock:alerts"

  val AlertaFlowId = "MyAlertaFlowId"

}
