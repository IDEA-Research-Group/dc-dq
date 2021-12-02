package es.us.idea.dcdq.diagnosis.cost

import es.us.idea.dcdq.diagnosis.graph.DMN4DQTree
import es.us.idea.dcdq.diagnosis.graph.components.Assessment
import play.api.libs.json.{Json, OFormat}

case class BRDVBasedCostModel(default: Double, brdvCosts: Seq[BRDVCost]) {

  def toJsonString(): String = {
    Json.stringify(Json.toJson(this))
  }
}
object BRDVBasedCostModel {
  implicit val brdvBasedCostModelFormat: OFormat[BRDVBasedCostModel] = Json.format[BRDVBasedCostModel]
  def deserializeJsonString(str: String): BRDVBasedCostModel = Json.parse(str).as[BRDVBasedCostModel]

  def generateTemplate(dmn4dqtree: DMN4DQTree, assessments: List[Assessment]): BRDVBasedCostModel = {
    val r = dmn4dqtree.brdvValuesRanking(assessments)

    BRDVBasedCostModel(default = 1.0, brdvCosts = r.toSeq.map( x => {
      BRDVCost(
        name = x._1,
        default = 1,
        costs = {
          val ranksAndValues = x._2.groupBy(_._1).mapValues(_.map(_._2))
          val ranks = ranksAndValues.keys.toList.distinct.sorted
          var result: Seq[ObservedToTargetCost] = Seq()
          ranks.foreach(rank => {
            val brdvsCurrentRank = ranksAndValues(rank)
            var upperRank = rank + 1
            while(upperRank <= ranks.max) {
              val brdvsUpperRank = ranksAndValues(upperRank)
              brdvsCurrentRank.foreach(currentBrdv => {
                brdvsUpperRank.foreach(upperBrdv => {
                  result = result :+ ObservedToTargetCost(currentBrdv.value, upperBrdv.value, cost = 1)
                })
              })
              upperRank = upperRank + 1
            }
          })
          result
        }
      )
    }))
  }

}

