package io.guanaco.alerta.util

/**
  * Created by gertv on 4/26/17.
  */
class AlertaConfig[T](val flowId: String, val services: Seq[String], val resource: T => String) {

}

object AlertaConfig {

  def apply[T](flowId: String, services: Seq[String])(resource: T => String): AlertaConfig[T] =
    new AlertaConfig(flowId, services, resource)

}
