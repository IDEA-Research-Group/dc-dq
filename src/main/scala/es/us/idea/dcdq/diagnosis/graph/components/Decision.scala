package es.us.idea.dcdq.diagnosis.graph.components

import es.us.idea.dcdq.diagnosis.graph.Tree
import es.us.idea.dcdq.diagnosis.graph.components.basic.Vertex
import play.api.libs.json.{JsObject, JsString}

class Decision(decision: String) extends Vertex {

  def decision(): String = decision

  override def toString: String = s"Decision@${id()}[value=$decision]"

  override def id(): String = decision.hashCode.toHexString

  override def getChildren(implicit tree: Tree): Set[Assessment] =
    super.getChildren.flatMap {
      case a: Assessment => Some(a)
      case _ => None
    }

  override def convert2json: JsObject =
    JsObject(Seq("type" -> JsString("Decision"), "decision" -> JsString(decision()), "id" -> JsString(id())))

}

object Decision {

  def apply(decision: String): Decision = new Decision(decision)

  def deserializeJson(jsObject: JsObject): Decision = new Decision(jsObject.value("decision").as[String])

}