package es.us.idea.dcdq.diagnosis.spark

import es.us.idea.dcdq.diagnosis.graph.DMN4DQTree
import org.apache.spark.sql.DataFrame

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
class DataFrameDQMonitor(df: DataFrame, dmn4dqTree: DMN4DQTree) {

  def df(): DataFrame = df





}