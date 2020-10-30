package io.guanaco.alerta.util

import io.guanaco.alerta.api.Alerta
import io.guanaco.alerta.util.AlertaRouteBuilderSupport.RESOURCE_PROPERTY
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.{ProcessorDefinition, RouteDefinition}
import org.apache.camel.spi.Synchronization
import org.apache.camel.{CamelException, Exchange, Header, Processor, ProducerTemplate}

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
    def configureAlerta[T](implicit config: AlertaConfig[T]): ProcessorDefinition[_] = {
      val synchronization = AlertaSynchronization(alerta)

      definition
        .process(new Processor {
          override def process(exchange: Exchange): Unit = {
            if (!exchange.getUnitOfWork.containsSynchronization(synchronization))
              exchange.getUnitOfWork.addSynchronization(synchronization)
          }
        })
        .process(ResourceProcessor(config))
    }

  }

  case class AlertaSynchronization[T](alerta: Alerta)(implicit config: AlertaConfig[T]) extends Synchronization {
    val helper = AlertaHelper[T](alerta)

    override def onComplete(exchange: Exchange): Unit =
      method(helper, "complete").evaluate(exchange, classOf[Unit])

    override def onFailure(exchange: Exchange): Unit =
      method(helper, "failed").evaluate(exchange, classOf[Unit])
  }

  /**
    * Configure Alerta integration
    *
    * @param config configuration for the Alerta integration
    * @tparam T the type of the message body
    */
  def configureAlerta[T](implicit config: AlertaConfig[T]): Unit = {
    val helper = AlertaHelper(alerta)

    intercept()
      .process(ResourceProcessor(config))

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
      val payload =  Option(exchange.getProperty(RESOURCE_PROPERTY, classOf[String])) match {
        case None => Body(Option(`override`) getOrElse body)
        case Some(value) => Fixed(value)
      }
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

      val payload =
        Option(`override`).map(Body(_)) orElse
        Option(exchange.getProperty(RESOURCE_PROPERTY).toString).map(Fixed(_)) getOrElse
        Body(body)

      val attrs   = Option(attributes) getOrElse Map.empty[String, String]

      if (warning != null) sendAlertaWarning(payload, warning, attrs)
      else sendAlertaSuccess(payload)
    }
  }

  case class ResourceProcessor[T](config: AlertaConfig[T]) extends Processor {

    override def process(exchange: Exchange): Unit = {
      val body = exchange.getIn.getBody
      try {
        if (!exchange.getProperties.containsKey(RESOURCE_PROPERTY)) {
          Option(body) map { value => exchange.setProperty(RESOURCE_PROPERTY, config.resource(value.asInstanceOf[T])) }
        }
      } catch {
        case e: ClassCastException => exchange.setProperty(RESOURCE_PROPERTY, s"UnmappedType:${body.getClass.getSimpleName}")
      }
    }
  }

}

object AlertaRouteBuilderSupport {

  final val WARNING_HEADER       = "AlertaWarningHeader"
  final val OVERRIDE_BODY_HEADER = "AlertaOverrideBodyHeader"
  final val ATTRIBUTES_HEADER    = "AlertaAttributesHeader"
  final val RESOURCE_PROPERTY    = "AlertaResourceProperty"
}
