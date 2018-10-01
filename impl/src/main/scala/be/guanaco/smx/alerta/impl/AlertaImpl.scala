package be.guanaco.smx.alerta.impl

import be.guanaco.smx.alerta.api.{ Alert, Alerta, Heartbeat }
import org.apache.camel.{ CamelContext, ProducerTemplate }

/**
  * Default implementation for {@link AlertaImpl} - uses ActiveMQ to asynchronously deliver the alerts and heartbeats
  * to Alerta.
  */
object AlertaImpl {
  /*
   * Get activemq:// endpoint for a queue name
   */
  def getEndpoint(queue: String): String = String.format("activemq://%s", queue)
}

class AlertaImpl(val context: CamelContext) extends Alerta {

  import Alerta._
  import spray.json._
  import AlertaJsonProtocol._

  private var template: ProducerTemplate = _

  override def sendHeartbeat(heartbeat: Heartbeat): Unit =
    if (heartbeat != null) {
      getTemplate.asyncSendBody(AlertaImpl.getEndpoint(HEARTBEAT_QUEUE_NAME), heartbeat.toJson.compactPrint)
    }

  override def sendAlert(alert: Alert): Unit = if (alert != null) {
    getTemplate.asyncSendBody(AlertaImpl.getEndpoint(ALERT_QUEUE_NAME), alert.toJson.compactPrint)
  }

  /*
     * Get or create a producer template
     */
  private def getTemplate = {
    if (template == null) template = context.createProducerTemplate
    template
  }
}