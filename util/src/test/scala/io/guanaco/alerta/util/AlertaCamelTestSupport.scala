package io.guanaco.alerta.util

import io.guanaco.alerta.api.{Alert, Alerta}
import io.guanaco.alerta.impl.AlertaJsonProtocol._
import org.apache.camel.Handler
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.test.junit4.CamelTestSupport

/**
  * Test support trait for Alerta util testing
  */
trait AlertaCamelTestSupport { self: CamelTestSupport =>

  import AlertaCamelTestSupport.MOCK_ALERTS

  def createAlertaRouteBuilder() = new RouteBuilder() {
    override def configure(): Unit = {
      from(s"activemq://${Alerta.ALERT_QUEUE_NAME}")
        .transform(method(Helper()))
        .to(MOCK_ALERTS)
    }
    case class Helper() {
      import spray.json._

      @Handler
      def transform(message: String) = message.parseJson.convertTo[Alert]
    }
  }

}

object AlertaCamelTestSupport {

  val MOCK_ALERTS = "mock:alerts"

}
