package es.us.idea.dcdq.diagnosis.graph.components.basic

import play.api.libs.json.{JsObject, JsString}

class DirectedEdge(source: Vertex, target: Vertex) extends Serializable {
  def source(): Vertex = source
  def target(): Vertex = target

  def canEqual(a: Any): Boolean = a.isInstanceOf[DirectedEdge]

  override def equals(that: Any): Boolean = {
    that match {
      case that: DirectedEdge => that.canEqual(this) && (that.source() == this.source()) &&
        (that.target() == this.target())
      case _ => false
    }
  }

  override def hashCode(): Int = this.source().hashCode() * this.target().hashCode() * 17

  override def toString: String = s"DirectedEdge[source=$source(), target=$target()]"

  def convert2json: JsObject = JsObject(Seq("source" -> JsString(source().id()), "target" -> JsString(target().id())))

}

object DirectedEdge {
  def apply(source: Vertex, target: Vertex): DirectedEdge = new DirectedEdge(source, target)
}