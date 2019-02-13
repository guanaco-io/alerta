package io.guanaco.alerta.impl

import io.guanaco.alerta.api.{Alert, Alerta, Heartbeat}
import AlertaImpl.getEndpoint
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.test.AvailablePortFinder
import org.junit.Assert._
import org.junit.Test
import spray.json._
import AlertaJsonProtocol._

/**
  * Test cases for [[AlertaRoutes]]
  */
object AlertaRoutesTest {
  private val PORT = AvailablePortFinder.getNextAvailable(10000)
  private val HTTP_URL = s"http://localhost:${PORT}/alerta"
  private val MOCK_ALERTS = "mock:alerts"
  private val MOCK_HEARTBEATS = "mock:heartbeats"
}

class AlertaRoutesTest extends AbstractAlertaTest {
  @Test
  @throws[InterruptedException]
  def testSendAlert(): Unit = {
    val mock = getMockEndpoint(AlertaRoutesTest.MOCK_ALERTS)
    mock.expectedMessageCount(1)

    val alert = Alert("resource", "event", Array("tag1"))
    sendBody(getEndpoint(Alerta.ALERT_QUEUE_NAME), alert.toJson.compactPrint)
    assertMockEndpointsSatisfied()
    import scala.collection.JavaConversions._
    for (exchange <- mock.getExchanges) {
      assertEquals("{\"event\":\"event\",\"service\":[\"tag1\"],\"resource\":\"resource\",\"environment\":\"Production\",\"severity\":\"minor\",\"timeout\":604800}", exchange.getIn.getBody(classOf[String]))
      assertEquals("application/json", exchange.getIn.getHeader(Exchange.CONTENT_TYPE))
    }
  }

  @Test
  @throws[InterruptedException]
  def testSendHeartbeat(): Unit = {
    val mock = getMockEndpoint(AlertaRoutesTest.MOCK_HEARTBEATS)
    mock.expectedMessageCount(1)

    val heartbeat = Heartbeat("origin", Array("tag1"), 100)
    sendBody(getEndpoint(Alerta.HEARTBEAT_QUEUE_NAME), heartbeat.toJson.compactPrint)
    assertMockEndpointsSatisfied()
    import scala.collection.JavaConversions._
    for (exchange <- mock.getExchanges) {
      assertEquals("{\"origin\":\"origin\",\"tags\":[\"tag1\"],\"timeout\":100}", exchange.getIn.getBody(classOf[String]))
      assertEquals("application/json", exchange.getIn.getHeader(Exchange.CONTENT_TYPE))
    }
  }

  @throws[Exception]
  override protected def createRouteBuilders: Array[RouteBuilder] = Array[RouteBuilder](new AlertaRoutes(AlertaRoutesTest.HTTP_URL, "Production"), new RouteBuilder() {
    @throws[Exception]
    override def configure(): Unit = {
      //FORMAT:off
      from(String.format("jetty:%s/alert", AlertaRoutesTest.HTTP_URL))
        .convertBodyTo(classOf[String])
        .to(AlertaRoutesTest.MOCK_ALERTS)

      from(String.format("jetty:%s/heartbeat", AlertaRoutesTest.HTTP_URL))
        .convertBodyTo(classOf[String])
        .to(AlertaRoutesTest.MOCK_HEARTBEATS)
      //FORMAT:on
    }
  })
}