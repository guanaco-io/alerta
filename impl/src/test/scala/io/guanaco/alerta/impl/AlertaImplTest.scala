package io.guanaco.alerta.impl

import io.guanaco.alerta.api.{Alert, Alerta}
import AlertaImpl.getEndpoint
import io.guanaco.alerta.api.Heartbeat
import org.apache.camel.builder.RouteBuilder
import org.junit.Test

/**
  * Test cases for {@link AlertaImpl}
  */
object AlertaImplTest {
  private val MOCK_ALERTS = "mock:alerts"
  private val MOCK_HEARTBEATS = "mock:heartbeats"
}

class AlertaImplTest extends AbstractAlertaTest {
  @Test
  @throws[Exception]
  def testSendAlert(): Unit = {
    getMockEndpoint(AlertaImplTest.MOCK_ALERTS).expectedMessageCount(1)
    val alerta = new AlertaImpl(context)
    alerta.sendAlert(new Alert("resource", "event", Array()))
    alerta.sendAlert(null.asInstanceOf[Alert])
    assertMockEndpointsSatisfied()
  }

  @Test
  @throws[Exception]
  def testSendHeartbeat(): Unit = {
    getMockEndpoint(AlertaImplTest.MOCK_HEARTBEATS).expectedMessageCount(1)
    val alerta = new AlertaImpl(context)
    alerta.sendHeartbeat(Heartbeat("origin1", Array("tag1"), 10000))
    alerta.sendHeartbeat(null.asInstanceOf[Heartbeat])
    assertMockEndpointsSatisfied()
  }

  @throws[Exception]
  override protected def createRouteBuilder = new RouteBuilder() {
    @throws[Exception]
    override def configure(): Unit = {
      from(getEndpoint(Alerta.ALERT_QUEUE_NAME)).to(AlertaImplTest.MOCK_ALERTS)
      from(getEndpoint(Alerta.HEARTBEAT_QUEUE_NAME)).to(AlertaImplTest.MOCK_HEARTBEATS)
    }
  }
}