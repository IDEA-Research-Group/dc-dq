package es.us.idea.dmn4spark.diagnosis

import es.us.idea.dmn4spark.diagnosis.graph.DMN4DQTree
import es.us.idea.dmn4spark.diagnosis.spark.SparkDQMonitorEngine
import org.apache.spark.sql.DataFrame



object DMN4DQ {
  implicit class DMN4DQ(df: DataFrame) {
    def dmn4dq(dmn4dqTree: DMN4DQTree): DiagnosisEngine = new DiagnosisEngine(df, dmn4dqTree)
    // TODO: reveice sorted list of assessments?
    def dmn4dq() = new SparkDQMonitorEngine(df)
  }
}
