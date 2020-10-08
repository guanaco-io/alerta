package io.guanaco.alerta.impl

import io.guanaco.alerta.api.{Alert, Alerta}
import io.guanaco.alerta.impl.AlertaImpl.getEndpoint
import io.guanaco.alerta.impl.AlertaRoutes._
import org.apache.camel.Exchange.CONTENT_TYPE
import org.apache.camel.{Handler, LoggingLevel, Message}
import org.apache.camel.builder.RouteBuilder

/**
  * Route to send alert/heartbeat messages from an ActiveMQ queue to Alerta using the Alerta API.
  * Cfr. http://docs.alerta.io/en/latest/api/reference.html
  */
class AlertaRoutes(val apiUrl: String, val environment: String, val apiKey: Option[String] = None, val timeout: Option[Long] = None) extends RouteBuilder {

  def this(apiUrl: String, environment: String, apiKey: String, timeout: Long) {
    this(apiUrl, environment, Option(apiKey).filterNot(_.isEmpty), Option(timeout).filter(_ > 0))
  }

  @throws[Exception]
  override def configure(): Unit = {
    val url = apiUrl.replaceAll("http://", "http4://")

    //format: OFF
    from(getEndpoint(Alerta.ALERT_QUEUE_NAME))
      .bean(Helper())
      .log(LoggingLevel.DEBUG, LogName, "Sending alert to Alerta API: ${body}")
      .to(String.format("%s/alert", url))

    from(getEndpoint(Alerta.HEARTBEAT_QUEUE_NAME))
      .setHeader(CONTENT_TYPE, constant("application/json"))
      .log(LoggingLevel.DEBUG, LogName, "Sending heartbeat to Alerta API: ${body}")
      .to(String.format("%s/heartbeat", url))
    //format: ON
  }

  case class Helper() {

    import spray.json._
    import AlertaJsonProtocol._

    @Handler
    def prepareHttpRequest(message: Message): Unit = {
      message.setHeader(CONTENT_TYPE, "application/json")
      apiKey.filterNot(_.isEmpty).map(message.setHeader("X-API-Key", _))

      val alrt = message.getBody(classOf[String]).parseJson.convertTo[Alert]
      val alert = timeout map {alrt.withTimeout(_)} getOrElse null

      val result = alert.environment match {
        case None                          => alert.withEnvironment(environment)
        case Some(env) if env.trim.isEmpty => alert.withEnvironment(environment)
        case _                             => alert
      }
      message.setBody(result.toJson.compactPrint)
    }
  }

}

object AlertaRoutes {

  val LogName: String = classOf[AlertaRoutes].getName
}
