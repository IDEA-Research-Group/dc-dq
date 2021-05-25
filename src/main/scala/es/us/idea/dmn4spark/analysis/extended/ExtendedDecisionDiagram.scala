package es.us.idea.dmn4spark.analysis.extended

import es.us.idea.dmn4spark.analysis.DecisionDiagram
import es.us.idea.dmn4spark.dmn.engine.SafeCamundaFeelEngineFactory
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
    apply(new FileInputStream(dmnModelPath), avoidDuplicatedConditions, feelEngineOpt)
  }

  def apply(is: InputStream, avoidDuplicatedConditions: Boolean, feelEngineOpt: Option[FeelEngine]): ExtendedDecisionDiagram = {
    ExtendedDecisionDiagram.apply(DecisionDiagram(is), avoidDuplicatedConditions, feelEngineOpt)
  }

  private def getFeelEngine(feelEngineOpt: Option[FeelEngine]): FeelEngine = {
    feelEngineOpt match {
      case Some(feelEngine) => feelEngine
      case _ => new SafeCamundaFeelEngineFactory().createInstance()
    }
  }

}