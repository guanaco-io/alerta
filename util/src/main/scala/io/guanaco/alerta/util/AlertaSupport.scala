package io.guanaco.alerta.util

import io.guanaco.alerta.api.Alert
import io.guanaco.alerta.api.Alerta

/**
  * Created by gertv on 4/26/17.
  */
trait AlertaSupport {

  import AlertaSupport._

  val alerta: Alerta

  def sendAlertaSuccess[T](body: T)(implicit config: AlertaConfig[T]): Unit = {
    val alert =
      createAlert(Success, body)
        .withSeverity("normal")

    alerta.sendAlert(alert)
  }

  def sendAlertaWarning[T](body: T, warning: String, attributes: Map[String, String] = Map.empty)(implicit config: AlertaConfig[T]): Unit = {
    val alert =
      createAlert(Warning, body)
        .withSeverity("warning")
        .withText(warning)
        .withAttributes(attributes)

    alerta.sendAlert(alert)
  }

  def sendAlertaFailure[T](body: T, exception: Throwable, attributes: Map[String, String] = Map.empty)(implicit config: AlertaConfig[T]): Unit = {
    val alert =
      createAlert(Failure, body)
        .withText(exception.getMessage)
        .withValue(exception.getClass.getSimpleName)
        .withAttributes(attributes)

    alerta.sendAlert(alert)
  }

  private def createAlert[T](status: Status, body: T)(implicit config: AlertaConfig[T]): Alert = {
    val resource = try {
      config.resource(body)
    } catch {
      case e: ClassCastException => s"UnmappedType:${body.getClass.getSimpleName}"
    }

    Alert(resource, event(status), config.services.toArray, correlate = Some(allEvents), attributes = config.attributes)
  }

}

object AlertaSupport {

  type Status = String
  val Failure: Status = "Failure"
  val Success: Status = "Success"
  val Warning: Status = "Warning"

  /**
    * List of all result statuses
    */
  val AllStatuses = Seq(Success, Failure, Warning)

  def event[T](status: Status)(implicit config: AlertaConfig[T]) = s"${config.flowId}$status"

  def allEvents[T](implicit config: AlertaConfig[T]): Seq[String] = AllStatuses map { status =>
    event(status)
  }
}
