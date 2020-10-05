package io.guanaco.alerta.api

/**
  * Represents an alert.
  */
case class Alert(
    resource: String,
    event: String,
    service: Array[String],
    environment: Option[String] = None,
    severity: String = "minor",
    text: Option[String] = None,
    value: Option[String] = None,
    correlate: Option[Seq[String]] = None,
    timeout: Long = 7 * 24 * 60 * 60 /* 7 days */,
    attributes: Map[String, String] = Map.empty,
    customer: Option[String] = None
) {

  def withText(text: String): Alert =
    Alert(resource, event, service, environment, severity, Some(text), value, correlate, timeout, attributes, customer)

  def withSeverity(severity: String): Alert =
    Alert(resource, event, service, environment, severity, text, value, correlate, timeout, attributes, customer)

  def withValue(value: String): Alert =
    Alert(resource, event, service, environment, severity, text, Some(value), correlate, timeout, attributes, customer)

  def withEnvironment(environment: String): Alert =
    Alert(resource, event, service, Some(environment), severity, text, value, correlate, timeout, attributes, customer)

  def withAttributes(attrs: Map[String, String]): Alert =
    Alert(resource, event, service, environment, severity, text, value, correlate, timeout, attributes ++ attrs, customer)

  def withAttribute(attribute: (String, String)): Alert =
    Alert(resource, event, service, environment, severity, text, value, correlate, timeout, attributes + attribute, customer)

  def withCustomer(customer: String): Alert =
    Alert(resource, event, service, environment, severity, text, value, correlate, timeout, attributes, Some(customer))
}
