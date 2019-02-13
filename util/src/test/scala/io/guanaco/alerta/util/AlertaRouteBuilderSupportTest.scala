package io.guanaco.alerta.util

import io.guanaco.alerta.api.Alert
import io.guanaco.alerta.impl.AlertaImpl
import AlertaRouteBuilderSupportTest._
import AlertaCamelTestSupport._
import org.apache.activemq.camel.component.ActiveMQComponent
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.JndiRegistry
import org.apache.camel.test.junit4.CamelTestSupport
import org.apache.camel._
import org.junit.Assert._
import org.junit.Test

import scala.collection.JavaConversions._

/**
  * Test cases for [[AlertaRouteBuilderSupport]]
  */
class AlertaRouteBuilderSupportTest extends CamelTestSupport with AlertaCamelTestSupport {

  @Test
  def testSuccess(): Unit = {
    getMockEndpoint(END).expectedMessageCount(1)
    val alerts = getMockEndpoint(MOCK_ALERTS)
    alerts.expectedMessageCount(1)

    template.sendBody(START, "working")

    assertMockEndpointsSatisfied()

    val alert = alerts.getExchanges.head.getIn.getBody(classOf[Alert])
    assertCorrelatedEvents(alert)
    assertEquals(s"${AlertaFlowId}Success", alert.event)
    assertEquals(s"${AlertaFlowId}:working", alert.resource)
    assertEquals("normal", alert.severity)
  }

  @Test
  def testSuccessUnmapped(): Unit = {
    getMockEndpoint(END).expectedMessageCount(1)
    val alerts = getMockEndpoint(MOCK_ALERTS)
    alerts.expectedMessageCount(1)

    template.sendBody(START, 1024)

    assertMockEndpointsSatisfied()

    val alert = alerts.getExchanges.head.getIn.getBody(classOf[Alert])
    assertCorrelatedEvents(alert)
    assertEquals(s"${AlertaFlowId}Success", alert.event)
    assertEquals(s"UnmappedType:Integer", alert.resource)
    assertEquals("normal", alert.severity)
  }

  @Test
  def testSuccessWithAlternativeBody(): Unit = {
    getMockEndpoint(END).expectedMessageCount(1)
    val alerts = getMockEndpoint(MOCK_ALERTS)
    alerts.expectedMessageCount(1)

    import AlertaRouteBuilderSupport.OVERRIDE_BODY_HEADER
    template.sendBodyAndHeader(START, "not-really-working", OVERRIDE_BODY_HEADER, "working")

    assertMockEndpointsSatisfied()

    val alert = alerts.getExchanges.head.getIn.getBody(classOf[Alert])
    assertCorrelatedEvents(alert)
    assertEquals(s"${AlertaFlowId}Success", alert.event)
    assertEquals(s"${AlertaFlowId}:working", alert.resource)
    assertEquals("normal", alert.severity)
  }

  @Test
  def testFailure(): Unit = {
    val alerts = getMockEndpoint(MOCK_ALERTS)
    alerts.expectedMessageCount(1)

    template.asyncSendBody(START, "broken")

    assertMockEndpointsSatisfied()

    val alert = alerts.getExchanges.head.getIn.getBody(classOf[Alert])
    assertCorrelatedEvents(alert)
    assertEquals(s"${AlertaFlowId}Failure", alert.event)
    assertEquals(s"${AlertaFlowId}:broken", alert.resource)
    assertEquals("minor", alert.severity)
    assertEquals("It's broken! It really is broken!!", alert.text.get)
    assertEquals("IllegalStateException", alert.value.get)
  }

  @Test
  def testWarning(): Unit = {
    val alerts = getMockEndpoint(MOCK_ALERTS)
    alerts.expectedMessageCount(1)

    template.asyncSend(START, new Processor() {
      override def process(exchange: Exchange): Unit = {
        exchange.getIn.setBody("warning")
        exchange.getIn.setHeader(AlertaRouteBuilderSupport.WARNING_HEADER, "Just a tiny bit messed up")
      }
    })

    assertMockEndpointsSatisfied()

    val alert = alerts.getExchanges.head.getIn.getBody(classOf[Alert])
    assertCorrelatedEvents(alert)
    assertEquals(s"${AlertaFlowId}Warning", alert.event)
    assertEquals(s"${AlertaFlowId}:warning", alert.resource)
    assertEquals("warning", alert.severity)
    assertEquals("Just a tiny bit messed up", alert.text.get)
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
        configureAlerta[String]

        //format: OFF
        from(START)
          .choice()
            .when(simple("${body} contains 'broken'"))
              .throwException(new IllegalStateException("It's broken! It really is broken!!"))
            .otherwise()
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

object AlertaRouteBuilderSupportTest {

  val START = "direct:start"

  val END = "mock:end"
  val FAILED = "mock:failed"
  val HANDLED = "mock:handled"

  val AlertaFlowId = "MyAlertaFlowId"

}
