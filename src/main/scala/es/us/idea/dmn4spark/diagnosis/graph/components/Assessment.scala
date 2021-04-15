package es.us.idea.dmn4spark.diagnosis.graph.components

import es.us.idea.dmn4spark.diagnosis.graph.{DMN4DQTree, Tree}
import es.us.idea.dmn4spark.diagnosis.graph.components.basic.Vertex
import play.api.libs.json.{JsObject, JsString}

class Assessment(value: String) extends Vertex {
  def value(): String = value

  override def toString: String = s"Assessment@$id[value=$value]"

  override def id(): String = value.hashCode.toHexString

  override def getChildren(implicit tree: Tree): Set[Measurement] =
    super.getChildren.flatMap {
      case x: Measurement => Some(x)
      case _ => None
    }

  override def getParents(implicit tree: Tree): Set[Decision] =
    super.getParents.flatMap {
      case x: Decision => Some(x)
      case _ => None
    }

  override def convert2json: JsObject =
    JsObject(Seq("type" -> JsString("Assessment"), "value" -> JsString(value()), "id" -> JsString(id())))

}

object Assessment {

  def apply(value: String): Assessment = new Assessment(value)

  def deserializeJson(jsObject: JsObject): Assessment = new Assessment(jsObject.value("value").as[String])


}