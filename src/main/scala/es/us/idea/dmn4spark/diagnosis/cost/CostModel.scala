package es.us.idea.dmn4spark.diagnosis.cost

case class CostModel(brdvCosts: Map[String, List[ObservedToTargetCost]], default: Double)
//case class BRDVCost(brdvName: String, observedToTargetCost: List[ObservedToTargetCost])
case class ObservedToTargetCost(observedValue: String, targetValue: String, cost: Double)