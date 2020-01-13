package io.guanaco.alerta.util

import io.guanaco.alerta.api.Alert
import io.guanaco.alerta.impl.AlertaImpl
import io.guanaco.alerta.test.AlertaCamelTestSupport
import io.guanaco.alerta.test.AlertaCamelTestSupport._
import org.apache.activemq.camel.component.ActiveMQComponent
import org.apache.camel._
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.JndiRegistry
import org.apache.camel.test.junit4.CamelTestSupport
import org.junit.Assert._
import org.junit.Test

import scala.collection.JavaConversions._

/**
  * Test cases for [[AlertaRouteBuilderSupport]] together with Camel error handlers
  */
class AlertaRouteBuilderSupportErrorHandlerTest extends CamelTestSupport with AlertaCamelTestSupport {

  import AlertaRouteBuilderSupportErrorHandlerTest._

  @Test
  def testAlwaysFail(): Unit = {
    val alerts = getMockEndpoint(MOCK_ALERTS)
    alerts.expectedMessageCount(1)

    template.asyncSendBody(DirectAlwaysFail, None)

    assertMockEndpointsSatisfied()

    val alert = alerts.getExchanges.head.getIn.getBody(classOf[Alert])
    assertEquals(s"${AlertaFlowId}Failure", alert.event)
    assertEquals("minor", alert.severity)
  }

  @Test
  def testSucceedAfterRetries(): Unit = {
    val alerts = getMockEndpoint(MOCK_ALERTS)
    alerts.expectedMessageCount(1)

    template.asyncSendBody(DirectSucceedAfterRetries, None)

    assertMockEndpointsSatisfied()

    val alert = alerts.getExchanges.head.getIn.getBody(classOf[Alert])
    assertEquals(s"${AlertaFlowId}Success", alert.event)
    assertEquals("normal", alert.severity)
  }

  override def createRouteBuilders(): Array[RoutesBuilder] =
    Array(
      createAlertaRouteBuilder(),
      new RouteBuilder() with AlertaRouteBuilderSupport {
        implicit val config = AlertaConfig(AlertaFlowId, Seq("service")) { value: String =>
          s"${AlertaFlowId}:${value}"
        }

        override def configure(): Unit = {
          configureAlerta[String]

          onException().redeliveryDelay(50).maximumRedeliveries(4).handled(false)

          val helper = Helper()

          // format: off
          from(DirectAlwaysFail)
            .bean(method(helper, "alwaysFail"))

          from(DirectSucceedAfterRetries)
            .bean(method(helper, "succeedOnFinalRetry"))
          // format: on
        }

        case class Helper() {

          def alwaysFail(): Unit =
            throw new RuntimeException("Bad things happen! We can't deal with them!")

          def succeedOnFinalRetry(message: Message): Unit = {
            val redelivery = Option(message.getHeader(Exchange.REDELIVERY_COUNTER, classOf[Int])).getOrElse(0)
            val maxRedeliveries = Option(message.getHeader(Exchange.REDELIVERY_MAX_COUNTER, classOf[Int])).getOrElse(Int.MaxValue)

            if (redelivery < maxRedeliveries) throw new RuntimeException("Bad things happen! We can't deal with them!")
          }

        }
      }
    )

  override def createCamelContext(): CamelContext = {
    val context = super.createCamelContext()
    context.addComponent("activemq", ActiveMQComponent.activeMQComponent("vm://test?broker.persistent=false&broker.useJmx=false"))
    context.getRegistry(classOf[JndiRegistry]).bind("alerta", new AlertaImpl(context))
    context
  }
}

object AlertaRouteBuilderSupportErrorHandlerTest {

  val DirectAlwaysFail = "direct:always-fail"

  val DirectSucceedAfterRetries = "direct:succeed-after-retries"

  val AlertaFlowId = "FlowWithErrorHandling"

}
