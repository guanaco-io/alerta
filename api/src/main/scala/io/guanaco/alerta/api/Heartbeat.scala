package io.guanaco.alerta.api

/**
  * Represents a heartbeat
  */
case class Heartbeat(origin: String, tags: Seq[String], timeout: Int)
