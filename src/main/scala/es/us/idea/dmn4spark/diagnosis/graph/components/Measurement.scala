package es.us.idea.dmn4spark.diagnosis.graph.components

import java.util.UUID.randomUUID

import es.us.idea.dmn4spark.diagnosis.graph.components.basic.{AndVertex, Vertex}
import play.api.libs.json.{JsObject, JsString}

class Measurement(id: String) extends AndVertex {
  def id(): String = id

  override def toString: String = s"Measurement@$id[]"

  override def convert2json: JsObject = JsObject(Seq("type" -> JsString("Measurement"), "id" -> JsString(id())))
}

object Measurement {

  def apply(id: String): Measurement = new Measurement(id)
  def apply(dimensionMeasurement: List[DimensionMeasurement]): Measurement =
    new Measurement(s"Observation[${dimensionMeasurement.sorted.mkString(":")}]".hashCode.toHexString)
  def apply(): Measurement = new Measurement(randomUUID().toString)
  
}
