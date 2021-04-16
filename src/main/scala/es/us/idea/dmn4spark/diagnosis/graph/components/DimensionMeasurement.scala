package es.us.idea.dmn4spark.diagnosis.graph.components

import es.us.idea.dmn4spark.diagnosis.graph.Tree
import es.us.idea.dmn4spark.diagnosis.graph.components.basic.Vertex
import play.api.libs.json.{JsObject, JsString}

class DimensionMeasurement(dimensionName: String, measuredValue: String) extends Vertex with Ordered[DimensionMeasurement] {
  def dimensionName(): String = dimensionName
  def measuredValue(): String = measuredValue
  def id(): String = s"$dimensionName:$measuredValue".hashCode.toHexString

  override def getChildren(implicit tree: Tree): Set[Observation] =
    super.getChildren.flatMap {
      case x: Observation => Some(x)
      case _ => None
    }

  override def getParents(implicit tree: Tree): Set[Measurement] =
    super.getParents.flatMap {
      case x: Measurement => Some(x)
      case _ => None
    }

  override def toString: String =
    s"DimensionMeasurement@$id[dimensionName=$dimensionName, measuredValue=$measuredValue]"

  override def compare(that: DimensionMeasurement): Int = that.id() compare this.id()

  override def convert2json: JsObject =
    JsObject(
      Seq(
        "type" -> JsString("DimensionMeasurement"),
        "dimensionName" -> JsString(dimensionName()),
        "measuredValue" -> JsString(measuredValue()),
        "id" -> JsString(id())
      )
    )
}

object DimensionMeasurement {

//  def apply(dimensionName: String, measuredValue: String, id: String): DimensionMeasurement =
//    new DimensionMeasurement(dimensionName, measuredValue, id)
  def apply(dimensionName: String, measuredValue: String): DimensionMeasurement =
    new DimensionMeasurement(dimensionName, measuredValue)

  def deserializeJson(jsObject: JsObject): DimensionMeasurement = {
    val m = jsObject.value
    new DimensionMeasurement(m("dimensionName").as[String], m("measuredValue").as[String])
  }

}