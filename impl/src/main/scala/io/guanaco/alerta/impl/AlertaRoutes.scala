package io.guanaco.alerta.impl

import io.guanaco.alerta.api.{Alert, Alerta}
import AlertaImpl.getEndpoint
import org.apache.camel.Exchange.CONTENT_TYPE
import org.apache.camel.{Body, Handler}
import org.apache.camel.builder.RouteBuilder

/**
  * Route to send alert/heartbeat messages from an ActiveMQ queue to Alerta using the Alerta API.
  * Cfr. http://docs.alerta.io/en/latest/api/reference.html
  */
class AlertaRoutes(val apiUrl: String, val environment: String) extends RouteBuilder {

  @throws[Exception]
  override def configure(): Unit = {
    val url = apiUrl.replaceAll("http://", "http4://")

    //format: OFF
    from(getEndpoint(Alerta.ALERT_QUEUE_NAME))
      .transform(method(Helper()))
      .setHeader(CONTENT_TYPE, constant("application/json"))
      .to(String.format("%s/alert", url))

    from(getEndpoint(Alerta.HEARTBEAT_QUEUE_NAME))
      .setHeader(CONTENT_TYPE, constant("application/json"))
      .to(String.format("%s/heartbeat", url))
    //format: ON
  }

  case class Helper() {

    import spray.json._
    import AlertaJsonProtocol._

    @Handler
    def addDefaults(@Body message: String): String = {
      val alert = message.parseJson.convertTo[Alert]
      val result = alert.environment match {
        case None => alert.withEnvironment(environment)
        case Some(env) if env.trim.isEmpty => alert.withEnvironment(environment)
        case _ => alert
      }
      result.toJson.compactPrint
    }
  }

}