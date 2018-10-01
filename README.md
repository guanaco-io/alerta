# Alerta

ServiceMix bundles adding error handling with [Alerta](https://alerta.io/)


## Installation
(Tested with ServiceMix version 7.0.0.M2 )

first, start an Alerta instance:
to set up a local Alerta instance with docker-compose, follow [this guideline](https://github.com/guanaco-io/alerta/wiki/Bootstrap-a-local-Alerta-instance-with-docker-compose)

install and start the bundles in your karaf console:
```
./bin/servicemix
> bundle:install mvn:be.guanaco.smx.alerta/api/1.0.0</bundle>
> bundle:install mvn:be.guanaco.smx.alerta/impl/1.0.0</bundle>
```

add a be.guanaco.smx.alerta.cfg file in <smx_install>/etc with your Alerta settings:
```ini
apiUrl = http://localhost:8282/api
environment = Development
```

## Getting Started

Add the guanaco-io/alerta dependency to your project.
eg using maven:
```xml
<dependency>
    <groupId>be.guanaco.smx.alerta</groupId>
    <artifactId>api</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>be.guanaco.smx.alerta</groupId>
    <artifactId>util</artifactId>
    <version>1.0.0</version>
</dependency>
```

in your osgi config, access the Alerta service and pass it to your class sending the alert:
```xml
<reference id="alerta" interface="be.guanaco.smx.alerta.Alerta"/>

<bean id="yourService" class="com.example.YourService">
    <argument ref="alerta" />
</bean>

```

## Usage

to create and send an Alert from anywhere in your code, use:
```scala

package com.example

import be.guanaco.smx.alerta.api.Alerta
import be.guanaco.smx.alerta.util.{AlertaConfig, AlertaSupport}

class YourService(val alerta: Alerta) extends AlertaSupport {

  case class DomainObject(id: String, content: String)

  implicit val alertaConfig = AlertaConfig("your-flow-id", Seq("service1", "service2")) { domainObject: DomainObject =>
    domainObject.id
  }

  Try {
    veryRiskyOperation(domainObject)
  } match {
    case Success(_)  => sendAlertaSuccess(domainObject)
    case Failure(ex) => sendAlertaFailure(domainObject, e)
  }
}
```
because we also send a success alert, this snippet can be repeated and previously failed attempts of veryRiskyOperation will eventually resolve, causing the Alert to close automatically in Alerta.



to add error handling support in your camel RouteBuilder
for more examples see [AlertaRouteBuilderSupportTest](https://github.com/guanaco-io/alerta/blob/master/util/src/test/scala/be/guanaco/smx/alerta/util/AlertaRouteBuilderSupportTest.scala))
```scala

import be.guanaco.smx.alerta.util.{AlertaConfig, AlertaRouteBuilderSupport}

new RouteBuilder() with AlertaRouteBuilderSupport {
  implicit val config = AlertaConfig(AlertaFlowId, Seq("service")) { value: String =>
    s"${AlertaFlowId}:${value}"
  }

  override def configure(): Unit = {
    configureAlerta[String]

    //format: OFF
    from(START)
      .choice()
        .when(simple("${body} contains 'broken'"))
          .throwException(new IllegalStateException("It's broken! It really is broken!!"))
        .otherwise()
          .to(END)
    //format: ON
  }
}
```
