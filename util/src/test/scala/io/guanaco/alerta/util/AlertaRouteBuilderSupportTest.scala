package io.guanaco.alerta.util

import io.guanaco.alerta.api.Alert
import io.guanaco.alerta.impl.AlertaImpl
import io.guanaco.alerta.test.AlertaCamelTestSupport
import io.guanaco.alerta.test.AlertaCamelTestSupport._
import io.guanaco.alerta.util.AlertaRouteBuilderSupport._
import io.guanaco.alerta.util.AlertaRouteBuilderSupportTest._
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.test.junit4.CamelTestSupport
import org.apache.camel._
import org.apache.camel.component.activemq.ActiveMQComponent
import org.junit.Assert._
import org.junit.Test

import scala.collection.JavaConverters._

/**
  * Test cases for [[AlertaRouteBuilderSupport]]
  */
class AlertaRouteBuilderSupportTest extends CamelTestSupport with AlertaCamelTestSupport {

  @Test
  def testSuccess(): Unit = {
    getMockEndpoint(END).expectedMessageCount(1)
    val alerts = getMockEndpoint(MOCK_ALERTS)
    alerts.expectedMessageCount(1)

    template.sendBodyAndHeader(START, "working", ATTRIBUTES_HEADER, Map("test" -> "value"))

    assertMockEndpointsSatisfied()

    val alert = alerts.getExchanges.asScala.head.getIn.getBody(classOf[Alert])
    assertCorrelatedEvents(alert)
    assertEquals(s"${AlertaFlowId}Success", alert.event)
    assertEquals(s"${AlertaFlowId}:working", alert.resource)
    assertEquals("normal", alert.severity)
    assertEquals(AttributesStatic, alert.attributes)
  }

  @Test
  def testSuccessUnmapped(): Unit = {
    getMockEndpoint(END).expectedMessageCount(1)
    val alerts = getMockEndpoint(MOCK_ALERTS)
    alerts.expectedMessageCount(1)

    template.sendBody(START, 1024)

    assertMockEndpointsSatisfied()

    val alert = alerts.getExchanges.asScala.head.getIn.getBody(classOf[Alert])
    assertCorrelatedEvents(alert)
    assertEquals(s"${AlertaFlowId}Success", alert.event)
    assertEquals(s"UnmappedType:Integer", alert.resource)
    assertEquals("normal", alert.severity)
    assertEquals(AttributesStatic, alert.attributes)
  }

  @Test
  def testSuccessWithAlternativeBody(): Unit = {
    getMockEndpoint(END).expectedMessageCount(1)
    val alerts = getMockEndpoint(MOCK_ALERTS)
    alerts.expectedMessageCount(1)

    import AlertaRouteBuilderSupport.OVERRIDE_BODY_HEADER
    template.sendBodyAndHeader(START, "not-really-working", OVERRIDE_BODY_HEADER, "working")

    assertMockEndpointsSatisfied()

    val alert = alerts.getExchanges.asScala.head.getIn.getBody(classOf[Alert])
    assertCorrelatedEvents(alert)
    assertEquals(s"${AlertaFlowId}Success", alert.event)
    assertEquals(s"${AlertaFlowId}:working", alert.resource)
    assertEquals("normal", alert.severity)
    assertEquals(AttributesStatic, alert.attributes)
  }

  @Test
  def testFailure(): Unit = {
    val alerts = getMockEndpoint(MOCK_ALERTS)
    alerts.expectedMessageCount(1)

    template.asyncSendBody(START, "broken")

    assertMockEndpointsSatisfied()

    val alert = alerts.getExchanges.asScala.head.getIn.getBody(classOf[Alert])
    assertCorrelatedEvents(alert)
    assertEquals(s"${AlertaFlowId}Failure", alert.event)
    assertEquals(s"${AlertaFlowId}:broken", alert.resource)
    assertEquals("minor", alert.severity)
    assertEquals("It's broken! It really is broken!!", alert.text.get)
    assertEquals("IllegalStateException", alert.value.get)
    assertEquals(AttributesStatic ++ AttributesDynamic, alert.attributes)
  }

  @Test
  def testWarning(): Unit = {
    val alerts = getMockEndpoint(MOCK_ALERTS)
    alerts.expectedMessageCount(1)

    template.asyncSend(
      START,
      new Processor() {
        override def process(exchange: Exchange): Unit = {
          exchange.getIn.setBody("warning")
          exchange.getIn.setHeader(AlertaRouteBuilderSupport.WARNING_HEADER, "Just a tiny bit messed up")
          exchange.getIn.setHeader(AlertaRouteBuilderSupport.ATTRIBUTES_HEADER, AttributesDynamic)
        }
      }
    )

    assertMockEndpointsSatisfied()

    val alert = alerts.getExchanges.asScala.head.getIn.getBody(classOf[Alert])
    assertCorrelatedEvents(alert)
    assertEquals(s"${AlertaFlowId}Warning", alert.event)
    assertEquals(s"${AlertaFlowId}:warning", alert.resource)
    assertEquals("warning", alert.severity)
    assertEquals("Just a tiny bit messed up", alert.text.get)
    assertEquals(AttributesStatic ++ AttributesDynamic, alert.attributes)
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
        implicit val config = AlertaConfig(AlertaFlowId, Seq("service"), AttributesStatic) { value: String =>
          s"${AlertaFlowId}:${value}"
        }

        override def configure(): Unit = {
          configureAlerta[String]

          // format: off
          from(START)
            .choice()
              .when(simple("${body} contains 'broken'"))
                .setHeader(ATTRIBUTES_HEADER, constant(AttributesDynamic))
                .throwException(new IllegalStateException("It's broken! It really is broken!!"))
              .otherwise()
                .to(END)
          // format: on
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

object AlertaRouteBuilderSupportTest {

  val START = "direct:start"

  val END     = "mock:end"
  val FAILED  = "mock:failed"
  val HANDLED = "mock:handled"

  val AlertaFlowId = "MyAlertaFlowId"

  val AttributesStatic  = Map("static" -> "attr")
  val AttributesDynamic = Map("retry"  -> "path")
}
