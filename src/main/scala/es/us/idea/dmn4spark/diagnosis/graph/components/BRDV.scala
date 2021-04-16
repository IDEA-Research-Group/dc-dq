package es.us.idea.dmn4spark.diagnosis.graph.components

import es.us.idea.dmn4spark.analysis.model.Value
import es.us.idea.dmn4spark.diagnosis.graph.Tree
import es.us.idea.dmn4spark.diagnosis.graph.components.basic.{AndVertex, Leaf, Vertex}
import play.api.libs.json.{JsObject, JsString}

import scala.math.Ordered.orderingToOrdered

class BRDV(name: String, value: String) extends Value(name, value, "string") with AndVertex
  with Ordered[BRDV] {

  override def id(): String = s"$name:$value".hashCode.toHexString

  override def getChildren(implicit tree: Tree): Set[Attribute] =
    super.getChildren.flatMap {
      case x: Attribute => Some(x)
      case _ => None
    }

  override def getParents(implicit tree: Tree): Set[Observation] =
    super.getParents.flatMap {
      case x: Observation => Some(x)
      case _ => None
    }

  override def toString: String = s"BRDV@$id[name=$name, value=$value]"

  override def canEqual(a: Any): Boolean = super[AndVertex].canEqual(a)
  override def equals(that: Any): Boolean = super[AndVertex].equals(that)
  override def hashCode(): Int = super[AndVertex].hashCode()

  override def compare(that: BRDV): Int = this.id compare that.id

  override def convert2json: JsObject = JsObject(
    Seq(
      "type" -> JsString("BRDV"),
      "name" -> JsString(name),
      "value" -> JsString(value),
      "id" -> JsString(id())
    )
  )
}

object BRDV {
  //def apply(name: String, value: String, id: String): BRDV = new BRDV(name, value, id)
  def apply(name: String, value: String): BRDV = new BRDV(name, value)
  def deserializeJson(jsObject: JsObject): BRDV = {
    val m = jsObject.value
    new BRDV(m("name").as[String], m("value").as[String])
  }
}