package es.us.idea.dmn4spark.diagnosis.graph.components

import java.util.UUID.randomUUID

import es.us.idea.dmn4spark.diagnosis.graph.Tree
import es.us.idea.dmn4spark.diagnosis.graph.components.basic.{AndVertex, Vertex}
import play.api.libs.json.{JsObject, JsString}

class Measurement(id: String) extends AndVertex {
  def id(): String = id

  override def toString: String = s"Measurement@$id[]"

  override def getChildren(implicit tree: Tree): Set[DimensionMeasurement] =
    super.getChildren.flatMap {
      case x: DimensionMeasurement => Some(x)
      case _ => None
    }

  override def getParents(implicit tree: Tree): Set[Assessment] =
    super.getParents.flatMap {
      case x: Assessment => Some(x)
      case _ => None
    }

  override def convert2json: JsObject = JsObject(Seq("type" -> JsString("Measurement"), "id" -> JsString(id())))
}

object Measurement {

  def apply(id: String): Measurement = new Measurement(id)
  def apply(dimensionMeasurement: Set[DimensionMeasurement]): Measurement =
    Measurement(dimensionMeasurement.hashCode.toHexString)
    //new Measurement(s"Observation[${dimensionMeasurement.mkString(":")}]".hashCode.toHexString)
  def apply(): Measurement = new Measurement(randomUUID().toString)

  def deserializeJson(jsObject: JsObject): Measurement = new Measurement(jsObject.value("id").as[String])

}
