package es.us.idea.dcdq.diagnosis.cost

case class OldCostModel(brdvCosts: Map[String, List[OldObservedToTargetCost]], default: Double)
case class OldObservedToTargetCost(observedValue: String, targetValue: String, cost: Double)