package es.us.idea.dmn4spark.analysis

import org.camunda.bpm.dmn.feel.impl.FeelEngine

class ExtendedDecisionDiagram(extendedDMNTables: List[ExtendedDMNTable]) {
  def extendedDMNTables(): List[ExtendedDMNTable] = extendedDMNTables
}

object ExtendedDecisionDiagram {
  def apply(decisionDiagramSummary: DecisionDiagram, feelEngine: FeelEngine) =
    new ExtendedDecisionDiagram(
      decisionDiagramSummary.dmnTablesSummaries()
        .map(summary => ExtendedDMNTable(summary, decisionDiagramSummary, feelEngine))
    )

}