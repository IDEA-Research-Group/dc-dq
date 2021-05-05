package es.us.idea.dmn4spark.diagnosis

import es.us.idea.dmn4spark.diagnosis.cost.{OldCostModel, OldObservedToTargetCost}
import es.us.idea.dmn4spark.diagnosis.graph.DMN4DQTree
import es.us.idea.dmn4spark.diagnosis.graph.components.BRDV

class NewDiagnosis(targetBranch: DMN4DQTree, observedBranch: DMN4DQTree) {

  private def getBRDVS(dmn4dqtree: DMN4DQTree) = {
    dmn4dqtree.vertices().filter(_.isInstanceOf[BRDV]).map(_.asInstanceOf[BRDV])
  }

  def getBRDVDeviation(): (Double, Map[String, OldObservedToTargetCost]) = {
    val targetBrdvs = getBRDVS(targetBranch)
    val observedBrdvs = getBRDVS(observedBranch)

    targetBrdvs.map(targetBrdv => {
      observedBrdvs.find(_.name == targetBrdv.name) match {
        case Some(observedBrdv) => {
          if(observedBrdv.value != targetBrdv.value) ()
        }
        case _ => None
      }
    })
    ???
  }

  def getCost(costModel: OldCostModel): Double = {
    val targetBrdvs = getBRDVS(targetBranch)
    val observedBrdvs = getBRDVS(observedBranch)

    targetBrdvs.map(targetBrdv => {
      observedBrdvs.find(_.name == targetBrdv.name) match {
        case Some(observedBrdv) => {
          if(observedBrdv.value != targetBrdv.value){
            costModel.brdvCosts.get(targetBrdv.name) match {
              case Some(x) => x.find(c => c.observedValue == observedBrdv.value && c.targetValue == targetBrdv.value).map(_.cost).getOrElse(costModel.default)
              case _ => costModel.default
            }
          } else 0.0
        }
        case _ => Double.MaxValue
      }
    }).sum

  }


}
