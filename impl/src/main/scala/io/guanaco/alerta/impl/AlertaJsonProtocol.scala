package io.guanaco.alerta.impl

import io.guanaco.alerta.api.{Alert, Heartbeat}
import io.guanaco.alerta.api.Heartbeat
import spray.json.DefaultJsonProtocol

/**
  * Spray JSON protocol definition for [[Alert]] and [[Heartbeat]]
  */
object AlertaJsonProtocol extends DefaultJsonProtocol {

  implicit val heartbeatFormat = jsonFormat3(Heartbeat)
  implicit val alertFormat     = jsonFormat11(Alert)

}
