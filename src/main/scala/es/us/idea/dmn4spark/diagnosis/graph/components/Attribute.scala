package es.us.idea.dmn4spark.diagnosis.graph.components

import es.us.idea.dmn4spark.diagnosis.graph.components.basic.Vertex
import play.api.libs.json.{JsObject, JsString}

class Attribute(name: String) extends Vertex
  with Ordered[Attribute] {

  override def id(): String = name.hashCode.toHexString

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