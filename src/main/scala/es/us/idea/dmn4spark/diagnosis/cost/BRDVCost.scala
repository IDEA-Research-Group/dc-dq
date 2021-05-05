package es.us.idea.dmn4spark.diagnosis.cost

import play.api.libs.json.{Json, OFormat}

case class BRDVCost(name: String, default: Option[Double], costs: Seq[ObservedToTargetCost]) {
  def toJsonString(): String = {
    Json.stringify(Json.toJson(this))
  }
}

object BRDVCost {
  implicit val brdvCostFormat: OFormat[BRDVCost] = Json.format[BRDVCost]
  def deserializeJsonString(str: String): BRDVCost = Json.parse(str).as[BRDVCost]
}