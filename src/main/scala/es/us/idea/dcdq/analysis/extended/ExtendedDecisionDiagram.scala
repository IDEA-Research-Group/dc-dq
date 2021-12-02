package es.us.idea.dcdq.analysis.extended

import es.us.idea.dcdq.analysis.DecisionDiagram
import es.us.idea.dmn4spark.dmn.engine.SafeCamundaFeelEngineFactory
import es.us.idea.dmn4spark.dmn.executor.DMNExecutor
import org.apache.commons.io.IOUtils
import org.camunda.bpm.dmn.feel.impl.FeelEngine

import java.io.{FileInputStream, InputStream}

class ExtendedDecisionDiagram(extendedDMNTables: List[ExtendedDMNTable], avoidDuplicatedConditions: Boolean ) {
  def extendedDMNTables(): List[ExtendedDMNTable] = extendedDMNTables
  def avoidDuplicated(): Boolean = avoidDuplicatedConditions
  def getLeafExtendedDMNTables(): List[ExtendedDMNTable] = extendedDMNTables.filter(_.isLeafTable())
  def getNoLeafExtendedDMNTables(): List[ExtendedDMNTable] = extendedDMNTables.filter(! _.isLeafTable())
  // override def toString: String =
  //   s"ExtendedDecisionDiagram \n**** Printing DMN table summaries ****\n${dmnTablesSummaries.mkString("\n")}"
}

object ExtendedDecisionDiagram {
  def apply(decisionDiagramSummary: DecisionDiagram, avoidDuplicatedConditions: Boolean, feelEngineOpt: Option[FeelEngine]): ExtendedDecisionDiagram =
    new ExtendedDecisionDiagram(
      decisionDiagramSummary.dmnTablesSummaries()
        .map(summary => ExtendedDMNTable(summary, decisionDiagramSummary, avoidDuplicatedConditions,
          getFeelEngine(feelEngineOpt))),
      avoidDuplicatedConditions
    )

  def apply(dmnModelPath: String, avoidDuplicatedConditions: Boolean, feelEngineOpt: Option[FeelEngine] = None): ExtendedDecisionDiagram = {
    //ExtendedDecisionDiagram.apply(DecisionDiagram(dmnModelPath), avoidDuplicatedConditions, feelEngineOpt)
    apply(IOUtils.toByteArray(new FileInputStream(dmnModelPath)), avoidDuplicatedConditions, feelEngineOpt)
  }

  def apply(arrBytes: Array[Byte], avoidDuplicatedConditions: Boolean, feelEngineOpt: Option[FeelEngine]): ExtendedDecisionDiagram = {
    ExtendedDecisionDiagram.apply(DecisionDiagram(arrBytes), avoidDuplicatedConditions, feelEngineOpt)
  }

  def apply(dmnExecutor: DMNExecutor, avoidDuplicatedConditions: Boolean, feelEngineOpt: Option[FeelEngine]): ExtendedDecisionDiagram = {
    ExtendedDecisionDiagram.apply(DecisionDiagram(dmnExecutor), avoidDuplicatedConditions, feelEngineOpt)
  }

  private def getFeelEngine(feelEngineOpt: Option[FeelEngine]): FeelEngine = {
    feelEngineOpt match {
      case Some(feelEngine) => feelEngine
      case _ => new SafeCamundaFeelEngineFactory().createInstance()
    }
  }

}