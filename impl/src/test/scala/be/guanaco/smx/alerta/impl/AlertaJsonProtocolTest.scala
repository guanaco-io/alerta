package be.guanaco.smx.alerta.impl

import be.guanaco.smx.alerta.api.{ Alert, Heartbeat }
import org.apache.commons.io.IOUtils
import org.junit.Test
import org.junit.Assert._

/**
  * Test cases for [[AlertaJsonProtocol]]
  */
class AlertaJsonProtocolTest {

  import spray.json._
  import AlertaJsonProtocol._

  @Test
  def testSprayHeartbeat(): Unit = {
    val heartbeat = Heartbeat("origin1", Array("tag1"), 10000)

    val result = heartbeat.toJson.compactPrint
    val expected = readExpected("heartbeat.json")
    assertEquals(expected, result)
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
    assertEquals(expected, result)
  }

  def readExpected(name: String): String =
    IOUtils.toString(getClass.getClassLoader.getResourceAsStream(s"expected/${name}")).trim

}
