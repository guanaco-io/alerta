package io.guanaco.alerta.util

/*
 * sealed trait to add body if property is not available
 */
trait ResourceNameStrategy {}
case class Body[T](value: T)    extends ResourceNameStrategy
case class Fixed(value: String) extends ResourceNameStrategy
