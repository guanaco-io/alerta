package be.guanaco.smx.alerta.util

import be.guanaco.smx.alerta.api.Alerta
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.RouteDefinition
import org.apache.camel.{ Body, Handler, Header }

import scala.collection.JavaConversions._

/**
  * Convenience trait to add support for Alerta to a Camel [[RouteBuilder]]
  */
trait AlertaRouteBuilderSupport { self: RouteBuilder =>

  /*
   * Implicit conversion to add Alerta DSL to the RouteBuilder API
   */
  implicit class RichRouteDefintion(definition: RouteDefinition) {

    /**
      * Configure Alerta integration for a single route
      *
      * @param config configuration for the Alerta integration
      * @tparam T the type of the message body
      */
    def configureAlerta[T](implicit config: AlertaConfig[T]): RouteDefinition = {
      val helper = AlertaHelper(alerta)

      definition.onCompletion().bean(helper).end()
      definition
    }

  }

  /**
    * Configure Alerta integration
    *
    * @param config configuration for the Alerta integration
    * @tparam T the type of the message body
    */
  def configureAlerta[T](implicit config: AlertaConfig[T]): Unit = {
    val helper = AlertaHelper(alerta)

    onCompletion().bean(helper)
  }

  def alerta: Alerta = getContext.getRegistry.findByType(classOf[Alerta]).toList match {
    case value :: Nil => value
    case list => throw new IllegalStateException(s"${list.size} instances of Alerta API found - we need exactly one instance")
  }

  /*
   * Helper class for sending the alerts
   */
  case class AlertaHelper[T](alerta: Alerta)(implicit config: AlertaConfig[T]) extends AlertaSupport {

    import AlertaRouteBuilderSupport._

    @Handler
    def handle(@Body body: T, @Header(OVERRIDE_BODY_HEADER) `override`: T, @Header(WARNING_HEADER) warning: String, exception: Exception): Unit = {
      val payload = Option(`override`) getOrElse body

      if (exception != null) sendAlertaFailure(payload, exception)
      else if (warning != null) sendAlertaWarning(payload, warning)
      else sendAlertaSuccess(payload)
    }

  }
}

object AlertaRouteBuilderSupport {

  final val WARNING_HEADER = "AlertaWarningHeader"
  final val OVERRIDE_BODY_HEADER = "AlertaOverrideBodyHeader"

}