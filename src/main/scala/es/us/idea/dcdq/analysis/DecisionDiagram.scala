package es.us.idea.dcdq.analysis

import java.io.{FileInputStream, InputStream}
import es.us.idea.dcdq.analysis
import es.us.idea.dcdq.dmn.DMNExecutor
import es.us.idea.dcdq.dmn.engine.SafeCamundaFeelEngineFactory
import org.apache.commons.io.IOUtils
import org.camunda.feel.FeelEngine

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

class DecisionDiagram(dmnTablesSummaries: List[DMNTable]) {
  def dmnTablesSummaries(): List[DMNTable] = dmnTablesSummaries

  override def toString: String =
    s"DecisionDiagramSummary\n**** Printing DMN table summaries ****\n${dmnTablesSummaries.mkString("\n")}"
}

object DecisionDiagram {

  def apply(dmnModelPath: String): DecisionDiagram = {
    apply(new FileInputStream(dmnModelPath))
  }

  def apply(is: InputStream): DecisionDiagram = {
    val dmnExecutor = new DMNExecutor(IOUtils.toByteArray(is))
    val modelInstasnce = dmnExecutor.dmnModelInstance
    val dmnEngine = dmnExecutor.dmnEngine
    val graph = dmnEngine.parseDecisionRequirementsGraph(modelInstasnce)
    new DecisionDiagram(graph.getDecisions.asScala.map(decision => DMNTable(decision)).toList)

  }

}