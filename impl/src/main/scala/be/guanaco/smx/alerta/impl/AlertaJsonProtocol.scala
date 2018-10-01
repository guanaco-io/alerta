package be.guanaco.smx.alerta.impl

import be.guanaco.smx.alerta.api.{ Alert, Heartbeat }
import spray.json.DefaultJsonProtocol

/**
  * Spray JSON protocol definition for [[Alert]] and [[Heartbeat]]
  */
object AlertaJsonProtocol extends DefaultJsonProtocol {

  implicit val heartbeatFormat = jsonFormat3(Heartbeat)
  implicit val alertFormat = jsonFormat9(Alert)

}
