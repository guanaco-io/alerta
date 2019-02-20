package io.guanaco.alerta.util

/**
  * Created by gertv on 4/26/17.
  */
class AlertaConfig[T](val flowId: String,
                      val services: Seq[String],
                      val attributes: Map[String, String] = Map.empty,
                      val resource: T => String) {

  def withAttributes(attrs: Map[String, String]): AlertaConfig[T] =
    AlertaConfig(flowId, services, attributes ++ attrs)(resource)
}

object AlertaConfig {

  def apply[T](flowId: String, services: Seq[String], attributes: Map[String, String] = Map.empty)(resource: T => String): AlertaConfig[T] =
    new AlertaConfig(flowId, services, attributes, resource)

}
