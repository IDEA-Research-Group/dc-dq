package es.us.idea.dcdq.diagnosis.cost.csp

import es.us.idea.dcdq.diagnosis.graph.UsabilityProfile
import play.api.libs.json.{Json, OWrites, Writes}

case class Solution(observedUsabilityProfile: UsabilityProfile, targetUsabilityProfile: Option[UsabilityProfile], actions: List[Action], ert: Long)
object Solution {
  import Action._
  implicit val solutionWrites: Writes[Solution] = new Writes[Solution] {
    def writes(solution: Solution) = Json.obj(
      "observedUsabilityProfileDot" -> solution.observedUsabilityProfile.dotRepresentation,
      "targetUsabilityProfileDot" -> solution.targetUsabilityProfile.map(_.dotRepresentation),
      "actions" -> solution.actions.map(x => Json.toJson(x)),
      "ert" -> solution.ert
    )
  }
}

case class Action(brdv: String, from: String, to: String, cost: Int)
object Action{
  implicit val actionWrites: OWrites[Action] = Json.writes[Action]
}