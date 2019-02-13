# Alerta

ServiceMix bundles adding error handling with [Alerta](https://alerta.io/)


## Installation
(Tested with ServiceMix version 7.0.0.M2 )

first, start an Alerta instance:
to set up a local Alerta instance with docker-compose, follow [this guideline](https://github.com/guanaco-io/alerta/wiki/Bootstrap-a-local-Alerta-instance-with-docker-compose)

install and start the bundles in your karaf console:
```
./bin/servicemix
> bundle:install mvn:io.guanaco.alerta/api_2.11/1.0.1-SNAPSHOT</bundle>
> bundle:install mvn:io.guanaco.alerta/impl_2.11/1.0.1-SNAPSHOT</bundle>
```

add a io.guanaco.alerta.cfg file in <smx_install>/etc with your Alerta settings:
```ini
apiUrl = http://localhost:8282/api
environment = Development
```

## Getting Started

Add the guanaco-io/alerta dependency to your project.
eg using maven:
```xml
<repositories>
    <repository>
        <id>bintray-guanaco-io-alerta</id>
        <url>https://dl.bintray.com/guanaco-io/alerta/</url>
    </repository>
</repositories>

<dependency>
    <groupId>io.guanaco.alerta</groupId>
    <artifactId>api_2.11</artifactId>
    <version>1.0.1-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>io.guanaco.alerta</groupId>
    <artifactId>util_2.11</artifactId>
    <version>1.0.1-SNAPSHOT</version>
</dependency>
```

in your osgi config, access the Alerta service and pass it to your class sending the alert:
```xml
<reference id="alerta" interface="io.guanaco.alerta.Alerta"/>

<bean id="yourService" class="com.example.YourService">
    <argument ref="alerta" />
</bean>

```

## Usage

to create and send an Alert from anywhere in your code, use:
```scala

package com.example

import io.guanaco.alerta.api.Alerta
import io.guanaco.alerta.util.{AlertaConfig, AlertaSupport}

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
for more examples see [AlertaRouteBuilderSupportTest](https://github.com/guanaco-io/alerta/blob/master/util/src/test/scala/io/guanaco/alerta/util/AlertaRouteBuilderSupportTest.scala))
```scala

import io.guanaco.alerta.util.{AlertaConfig, AlertaRouteBuilderSupport}

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
