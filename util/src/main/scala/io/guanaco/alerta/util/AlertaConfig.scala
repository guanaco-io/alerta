package io.guanaco.alerta.util

/**
  * Created by gertv on 4/26/17.
  */
class AlertaConfig[T](val flowId: String, val services: Seq[String], val attributes: Map[String, String] = Map.empty, val customer: Option[String] = None, val timeout: Option[Long] = None, val resource: T => String) {

  def withAttributes(attrs: Map[String, String]): AlertaConfig[T] =
    AlertaConfig(flowId, services, attributes ++ attrs, customer, timeout)(resource)
}

object AlertaConfig {

  def apply[T](flowId: String, services: Seq[String], attributes: Map[String, String] = Map.empty, customer: Option[String] = None, timeout: Option[Long] = None)(resource: T => String): AlertaConfig[T] =
    new AlertaConfig(flowId, services, attributes, customer, timeout, resource)

}
