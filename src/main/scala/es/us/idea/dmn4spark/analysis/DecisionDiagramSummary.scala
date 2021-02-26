package es.us.idea.dmn4spark.analysis

import java.io.FileInputStream

import es.us.idea.dmn4spark.analysis
import es.us.idea.dmn4spark.dmn.DMNExecutor
import es.us.idea.dmn4spark.dmn.engine.SafeCamundaFeelEngineFactory
import org.apache.commons.io.IOUtils
import org.camunda.feel.FeelEngine

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

class DecisionDiagramSummary(dmnTablesSummaries: List[DMNTableSummary]) {
  def dmnTablesSummaries(): List[DMNTableSummary] = dmnTablesSummaries

  override def toString: String = s"DecisionDiagramSummary\n**** Printing DMN table summaries ****\n${dmnTablesSummaries.mkString("\n")}"
}

object DecisionDiagramSummary {

  def apply(dmnModelPath: String): DecisionDiagramSummary = {
    val dmnExecutor = new DMNExecutor(IOUtils.toByteArray(new FileInputStream(dmnModelPath)))
    val modelInstasnce = dmnExecutor.dmnModelInstance
    val dmnEngine = dmnExecutor.dmnEngine
    val graph = dmnEngine.parseDecisionRequirementsGraph(modelInstasnce)
    new DecisionDiagramSummary(graph.getDecisions.asScala.map(decision => DMNTableSummary(decision)).toList)
  }

}