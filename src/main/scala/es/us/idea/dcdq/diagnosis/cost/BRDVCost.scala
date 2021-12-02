package es.us.idea.dcdq.diagnosis.cost

import play.api.libs.json.{Json, OFormat}

case class BRDVCost(name: String, costs: Seq[ObservedToTargetCost], default: Int, single: Boolean = true) {
  def toJsonString(): String = {
    Json.stringify(Json.toJson(this))
  }
}

object BRDVCost {
  implicit val brdvCostFormat: OFormat[BRDVCost] = Json.format[BRDVCost]
  def deserializeJsonString(str: String): BRDVCost = Json.parse(str).as[BRDVCost]
}