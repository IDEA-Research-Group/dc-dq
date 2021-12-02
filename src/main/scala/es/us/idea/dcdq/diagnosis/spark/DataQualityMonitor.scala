package es.us.idea.dcdq.diagnosis.spark

import com.mongodb.spark.MongoSpark
import es.us.idea.dcdq.diagnosis.cost.BRDVBasedCostModel
import es.us.idea.dcdq.diagnosis.cost.csp.UsabilityProfileTransformation
import es.us.idea.dcdq.diagnosis.graph.components.basic.Vertex
import es.us.idea.dcdq.diagnosis.graph.{DMN4DQTree, UsabilityProfile}
import es.us.idea.dcdq.diagnosis.graph.components.{Assessment, Decision}
import es.us.idea.dcdq.diagnosis.spark.persistence.PersistenceConfiguration
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.col
import play.api.libs.json.{JsObject, Json}

/**
 * Features (todo)
 * - get underlying df
 * - get usability profiles and counts (json representation + )
 * - generate basic cost model
 * - get usability profiles which meet certain conditions: BR.DUD (only get those existing in the DF??)
 * - insert cost model (json str)
 * - apply inserted cost model => generate new object: "DataFrameDQReparation"
 *
 * - DFDQMonitors can be created from: df + dqmn4dqtree
 *    - any dataframe including DQ results, trees, etc
 *    - DFDQMonitors, if they are saved, a row including only the full tree must be included.
 *
 *
 */
class DataQualityMonitor(instance: String, dataDf: DataFrame, metadataDf: DataFrame, treeDf: DataFrame, dmn4dqTree: DMN4DQTree) {

  def data(): DataFrame = dataDf
  def metadata(): DataFrame = metadataDf


  //.costmodel.generateTemplate
  //.costmodel.setFromUrl... etc
  def costModelTemplate(assessmentRanking: List[Assessment]): BRDVBasedCostModel = BRDVBasedCostModel.generateTemplate(dmn4dqTree, assessmentRanking)
  def setCostModel(brdvBasedCostModel: BRDVBasedCostModel) = ???
  def setCostModel(costModelJson: String) = BRDVBasedCostModel.deserializeJsonString(costModelJson)

  def generateRepairingRecipe(decisionToRepair: Decision, targetDecision: Vertex, costModel: BRDVBasedCostModel) = {
    val toRepair = metadataDf.filter(col("DMN.DUD") === decisionToRepair.decision()).select("BranchJson"/*, "Count"*/)
      .collect().map(r => UsabilityProfile.deserializeJson(Json.parse(r.getString(0)).as[JsObject]))

    val target = dmn4dqTree.findAllBranches(targetDecision)

    println(s"ToRepair: ${toRepair.length}  --  --  Target: ${target.length}")

    UsabilityProfileTransformation.findOptimalTargetUsabilityProfiles(toRepair.toList, target, costModel)
  }

  def save(persistenceConfiguration: PersistenceConfiguration) = {
    MongoSpark.save(metadataDf, persistenceConfiguration.metadataStorage)
    MongoSpark.save(dataDf, persistenceConfiguration.dataStorage)
  }




}

