package io.guanaco.alerta.impl

import io.guanaco.alerta.api.Alert
import io.guanaco.alerta.api.Heartbeat
import org.apache.commons.io.IOUtils
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import spray.json._
import AlertaJsonProtocol._

/**
  * Test cases for [[AlertaJsonProtocol]]
  */
class AlertaJsonProtocolTest {

  @Test
  def testSprayHeartbeat(): Unit = {
    val heartbeat = Heartbeat("origin1", Array("tag1"), 10000)

    val result = heartbeat.toJson.compactPrint
    val expected = readExpected("heartbeat.json")

    JSONAssert.assertEquals(expected, result, false)
  }

  @Test
  def testSprayAlert(): Unit = {
    val alert =
      Alert("MyResource", "MyEvent", Array("Service1", "Service2"),
        severity = "major",
        text = Some("Very important message"),
        correlate = Some(Seq("CorrelatedEvent1", "CorrelatedEvent2")),
        value = Some("MyValue"),
        environment = Some("Production"))

    val result = alert.toJson.compactPrint
    val expected = readExpected("alert.json")

    JSONAssert.assertEquals(expected, result, false)
  }

  def readExpected(name: String): String =
    IOUtils.toString(getClass.getClassLoader.getResourceAsStream(s"expected/${name}")).trim

}
