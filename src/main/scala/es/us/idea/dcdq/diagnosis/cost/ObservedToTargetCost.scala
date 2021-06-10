package es.us.idea.dcdq.diagnosis.cost

import play.api.libs.json.{Json, OFormat}

case class ObservedToTargetCost(observedValue: String, targetValue: String, cost: Double) {


  def toJsonString(): String = {
    Json.stringify(Json.toJson(this))
  }
}

object ObservedToTargetCost {
  implicit val observedToTargetCostFormat: OFormat[ObservedToTargetCost] = Json.format[ObservedToTargetCost]
  def deserializeJsonString(str: String): ObservedToTargetCost = Json.parse(str).as[ObservedToTargetCost]
}