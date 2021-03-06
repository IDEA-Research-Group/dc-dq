package es.us.idea.dcdq.diagnosis.graph.components

import es.us.idea.dcdq.diagnosis.graph.Tree
import es.us.idea.dcdq.diagnosis.graph.components.basic.Vertex
import play.api.libs.json.{JsObject, JsString}

class Attribute(name: String) extends Vertex
  with Ordered[Attribute] {

  override def id(): String = name.hashCode.toHexString

  def name(): String = name

  override def getParents(implicit tree: Tree): Set[BRDV] =
    super.getParents.flatMap {
      case x: BRDV => Some(x)
      case _ => None
    }

  override def toString: String = s"Attribute@$id[name=$name]"

  override def canEqual(a: Any): Boolean = super[Vertex].canEqual(a)
  override def equals(that: Any): Boolean = super[Vertex].equals(that)
  override def hashCode(): Int = super[Vertex].hashCode()

  override def compare(that: Attribute): Int = this.id compare that.id

  override def convert2json: JsObject = JsObject(
    Seq(
      "type" -> JsString("Attribute"),
      "name" -> JsString(name),
      "id" -> JsString(id())
    )
  )
}

object Attribute {
  def apply(name: String): Attribute = new Attribute(name)
  def deserializeJson(jsObject: JsObject): Attribute = {
    val m = jsObject.value
    new Attribute(m("name").as[String])
  }
}