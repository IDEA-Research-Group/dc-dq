package es.us.idea.dcdq.analysis.extended

import es.us.idea.dcdq.analysis.{DMNTable, DecisionDiagram}
import org.camunda.bpm.dmn.feel.impl.FeelEngine

class ExtendedDMNTable(dmnTableSummary: DMNTable, extendedRules: List[ExtendedRule], isLeafTable: Boolean) {

  def dmnTableSummary(): DMNTable = dmnTableSummary
  def extendedRules(): List[ExtendedRule] = extendedRules
  def isLeafTable(): Boolean = isLeafTable

}

object ExtendedDMNTable {

  def apply(dmnTableSummary: DMNTable, context: DecisionDiagram, avoidDuplicatedConditions: Boolean, feelEngine: FeelEngine) = {
    val allTableOutputs = context.dmnTablesSummaries().flatMap(x => x.outputValues())
    val tableInputs = dmnTableSummary.inputAttributes()
    // Primero, empareja cada input con sus posibles valores de salida
    //val inputValues = tableInputs.map(input => allTableOutputs.find(output => output.name == input.name))
    //  .filter(_.isDefined).map(_.get)
    val inputValues = tableInputs.flatMap(input => allTableOutputs.filter(output => output.name == input.name))
    val inputAttributesFound = inputValues.map(_.name).distinct

    if(inputAttributesFound.size < dmnTableSummary.inputAttributes().size) {
      new ExtendedDMNTable(dmnTableSummary, List(), true)
    } else {
      val extendedRules = dmnTableSummary.rules().flatMap(_.extendRule(inputValues, feelEngine))
      val finalExtendedRules =
        if(avoidDuplicatedConditions) removeDuplicatedConditionsByOrder(extendedRules) else extendedRules
      new ExtendedDMNTable(dmnTableSummary, finalExtendedRules, false)
    }
  }

  private def removeDuplicatedConditionsByOrder(extendedRules: List[ExtendedRule]) = {
    val extendedRulesSorted = extendedRules.sortBy(_.order())
    var result: List[ExtendedRule] = List()
    for(extendedRule <- extendedRulesSorted) {
      val exists = result.exists(er => er.conditions().equals(extendedRule.conditions()))
      if(!exists) result = result :+ extendedRule
    }
    result
  }

}