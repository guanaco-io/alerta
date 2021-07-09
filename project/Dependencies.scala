import sbt._

object Dependencies {

  object Version {
    val camelBinary = "3-9"
    val camel       = "3.9.0"
    val activemq    = "5.16.2"
    val scalatest   = "3.2.9"
    val junit       = "4.13.2"
    val junit_if    = "0.13.2"
    val gson        = "2.4"
    val spray       = "1.3.5"
    val jsonassert  = "1.5.0"
    val commons_io  = "2.4"
    val slf4j       = "1.7.25"
    val jaxb_api    = "2.3.1"
    val jaxb_impl   = "2.3.2"
  }

  val common = Seq(
    "org.slf4j" % "slf4j-api" % Version.slf4j
  )

  val tests = Seq(
    "org.scalatest"  %% "scalatest"      % Version.scalatest % Test,
    "junit"          % "junit"           % Version.junit     % Test,
    "com.github.sbt" % "junit-interface" % Version.junit_if  % Test exclude ("junit", "junit-dep"),
    "org.slf4j"      % "slf4j-log4j12"   % Version.slf4j     % Test
  )

  lazy val impl = tests ++ common ++ Seq(
    "org.apache.camel"     % "camel-core"      % Version.camel,
    "com.google.code.gson" % "gson"            % Version.gson,
    "io.spray"             %% "spray-json"     % Version.spray,
    "org.apache.camel"     % "camel-test"      % Version.camel % Test,
    "org.apache.camel"     % "camel-jetty"     % Version.camel % Test,
    "org.apache.camel"     % "camel-http"      % Version.camel % Test,
    "org.apache.camel"     % "camel-activemq"  % Version.camel % Test,
    "org.apache.activemq"  % "activemq-broker" % Version.activemq % Test,
    "commons-io"           % "commons-io"      % Version.commons_io % Test,
    "org.skyscreamer"      % "jsonassert"      % Version.jsonassert % Test,
    "javax.xml.bind"       % "jaxb-api"        % Version.jaxb_api,
    "com.sun.xml.bind"     % "jaxb-impl"       % Version.jaxb_impl
  )

  lazy val api = common ++ tests

  lazy val test = common ++ tests ++ Seq(
    "org.apache.camel" % "camel-core" % Version.camel,
    "org.apache.camel" % "camel-test" % Version.camel
  )

  lazy val util = common ++ tests ++ Seq(
    "org.apache.camel"    % "camel-core"      % Version.camel,
    "org.apache.camel"    % "camel-test"      % Version.camel % Test,
    "org.apache.camel"    % "camel-activemq"  % Version.camel % Test,
    "org.apache.activemq" % "activemq-broker" % Version.activemq % Test
  )

}
