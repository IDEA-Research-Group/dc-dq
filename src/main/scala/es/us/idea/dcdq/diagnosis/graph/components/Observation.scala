package es.us.idea.dcdq.diagnosis.graph.components
import java.util.UUID.randomUUID

import es.us.idea.dcdq.diagnosis.graph.Tree
import es.us.idea.dcdq.diagnosis.graph.components.basic.{AndVertex, Vertex}
import play.api.libs.json.{JsObject, JsString}

class Observation(id: String) extends AndVertex {
  def id(): String = id

  override def getChildren(implicit tree: Tree): Set[BRDV] =
    super.getChildren.flatMap {
      case x: BRDV => Some(x)
      case _ => None
    }

  override def getParents(implicit tree: Tree): Set[DimensionMeasurement] =
    super.getParents.flatMap {
      case x: DimensionMeasurement => Some(x)
      case _ => None
    }

  override def toString: String = s"Observation@$id[]"

  override def convert2json: JsObject = JsObject(Seq("type" -> JsString("Observation"), "id" -> JsString(id())))
}

object Observation{

  def apply(id: String): Observation = new Observation(id)
  def apply(): Observation = new Observation(randomUUID().toString)
  def apply(brdvs: Set[BRDV]): Observation = new Observation(
    //s"Observation[${brdvs.mkString(":")}]".hashCode.toHexString
    brdvs.hashCode.toHexString
  )

  def deserializeJson(jsObject: JsObject): Observation = new Observation(jsObject.value("id").as[String])

}
