package io.guanaco.alerta.impl

import io.guanaco.alerta.api.{Alert, Alerta}
import io.guanaco.alerta.impl.AbstractAlertaTest.readExpected
import io.guanaco.alerta.impl.AlertaImpl.getEndpoint
import org.apache.camel.test.AvailablePortFinder
import org.junit.Test
import spray.json._
import io.guanaco.alerta.impl.AlertaJsonProtocol._
import org.apache.camel.{Exchange, RoutesBuilder}
import org.apache.camel.builder.RouteBuilder
import org.junit.Assert.assertEquals
import org.skyscreamer.jsonassert.JSONAssert

import scala.collection.JavaConverters._

/**
  * Test cases for [[AlertaRoutes]] with API Key
  */
object AlertaRoutesApiKeyTest {
  private val PORT        = AvailablePortFinder.getNextAvailable()
  private val HTTP_URL    = s"http://localhost:${PORT}/alerta"
  private val MOCK_ALERTS = "mock:alerts"
}

class AlertaRoutesApiKeyTest extends AbstractAlertaTest {

  @Test
  @throws[InterruptedException]
  def testSendAlert(): Unit = {
    val mock = getMockEndpoint(AlertaRoutesApiKeyTest.MOCK_ALERTS)
    mock.expectedMessageCount(1)

    val alert = Alert("resource", "event", Array("tag1"))
    sendBody(getEndpoint(Alerta.ALERT_QUEUE_NAME), alert.toJson.compactPrint)
    assertMockEndpointsSatisfied()

    for (exchange <- mock.getExchanges.asScala) {
      val expected = readExpected("alert-minimal.json")
      val actual   = exchange.getIn.getBody(classOf[String])

      JSONAssert.assertEquals(expected, actual, false)
      assertEquals("application/json", exchange.getIn.getHeader(Exchange.CONTENT_TYPE))
      assertEquals("apiKey", exchange.getIn.getHeader("X-API-Key"))
    }
  }

  @throws[Exception]
  override protected def createRouteBuilders: Array[RoutesBuilder] = Array(
    new AlertaRoutes(AlertaRoutesApiKeyTest.HTTP_URL, "Production", "apiKey", 604800),
    new RouteBuilder() {
      @throws[Exception]
      override def configure(): Unit = {
        //FORMAT:off
        from(String.format("jetty:%s/alert", AlertaRoutesApiKeyTest.HTTP_URL))
          .convertBodyTo(classOf[String])
          .to(AlertaRoutesApiKeyTest.MOCK_ALERTS)
        //FORMAT:on
      }
    }
  )

}
