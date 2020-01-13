package io.guanaco.alerta.util

import io.guanaco.alerta.api.Alerta
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.RouteDefinition
import org.apache.camel.{Body, CamelException, Exchange, Handler, Header, Processor}

import scala.collection.JavaConverters._

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

      definition.onCompletion().onFailureOnly().bean(helper, "failure").end()
      definition.onCompletion().onCompleteOnly().bean(helper, "complete").end()
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

    onCompletion().onFailureOnly().bean(helper, "failed")
    onCompletion().onCompleteOnly().bean(helper, "complete")
  }

  def alerta: Alerta = getContext.getRegistry.findByType(classOf[Alerta]).asScala.toList match {
    case value :: Nil => value
    case list         => throw new IllegalStateException(s"${list.size} instances of Alerta API found - we need exactly one instance")
  }

  /*
   * Helper class for sending the alerts
   */
  case class AlertaHelper[T](alerta: Alerta)(implicit config: AlertaConfig[T]) extends AlertaSupport {

    import AlertaRouteBuilderSupport._

    def failed(
        @Body body: T,
        @Header(OVERRIDE_BODY_HEADER) `override`: T,
        @Header(ATTRIBUTES_HEADER) attributes: Map[String, String],
        exchange: Exchange
    ): Unit = {
      val payload = Option(`override`) getOrElse body
      val attrs   = Option(attributes) getOrElse Map.empty[String, String]
      val exception = (Option(exchange.getException) orElse Option(exchange.getProperty(Exchange.EXCEPTION_CAUGHT, classOf[Exception])) orElse Option(
        exchange.getProperty(Exchange.EXCEPTION_HANDLED, classOf[Exception])
      )) getOrElse {
        new CamelException(s"Exchange ${exchange.getExchangeId} is marked as failed, but no exception is available")
      }

      sendAlertaFailure(payload, exception, attrs)
    }

    def complete(
        @Body body: T,
        @Header(OVERRIDE_BODY_HEADER) `override`: T,
        @Header(WARNING_HEADER) warning: String,
        @Header(ATTRIBUTES_HEADER) attributes: Map[String, String],
        exchange: Exchange
    ): Unit = {
      val payload = Option(`override`) getOrElse body
      val attrs   = Option(attributes) getOrElse Map.empty[String, String]

      if (warning != null) sendAlertaWarning(payload, warning, attrs)
      else sendAlertaSuccess(payload)
    }
  }
}

object AlertaRouteBuilderSupport {

  final val WARNING_HEADER       = "AlertaWarningHeader"
  final val OVERRIDE_BODY_HEADER = "AlertaOverrideBodyHeader"
  final val ATTRIBUTES_HEADER    = "AlertaAttributesHeader"
}
