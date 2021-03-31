package es.us.idea.dmn4spark.diagnosis.graph.components
import java.util.UUID.randomUUID

import es.us.idea.dmn4spark.diagnosis.graph.components.basic.{AndVertex, Vertex}
import play.api.libs.json.{JsObject, JsString}

class Observation(id: String) extends AndVertex {
  def id(): String = id

  override def toString: String = s"Observation@$id[]"

  override def convert2json: JsObject = JsObject(Seq("type" -> JsString("Observation"), "id" -> JsString(id())))
}

object Observation{

  def apply(id: String): Observation = new Observation(id)
  def apply(): Observation = new Observation(randomUUID().toString)
  def apply(brdvs: List[BRDV]): Observation = new Observation(
    s"Observation[${brdvs.sorted.mkString(":")}]".hashCode.toHexString
  )

  def deserializeJson(jsObject: JsObject): Observation = new Observation(jsObject.value("id").as[String])

}
