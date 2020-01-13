package io.guanaco.alerta.api

/**
  * Interface to interact with the Alerta service
  */
object Alerta {

  /**
    * ActiveMQ queue name to send alert messages
    */
  val ALERT_QUEUE_NAME = "alerta.alerts"

  /**
    * ActiveMQ queue name to send heartbeat messages
    */
  val HEARTBEAT_QUEUE_NAME = "alerta.heartbeats"
}

trait Alerta {

  /**
    * Convenience method to send a heartbeat
    *
    * @param heartbeat the heartbeat
    */
  def sendHeartbeat(heartbeat: Heartbeat): Unit

  /**
    * Convenience method to send an alert
    *
    * @param alert the alert
    */
  def sendAlert(alert: Alert): Unit
}
