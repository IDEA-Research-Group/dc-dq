package es.us.idea.dmn4spark.analysis

import es.us.idea.dmn4spark.analysis.model.{ExtendedRule, Value}
import org.camunda.bpm.dmn.feel.impl.FeelEngine

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

class ExtendedDMNTable(dmnTableSummary: DMNTable, extendedRules: List[ExtendedRule], isLeafTable: Boolean) {

  def dmnTableSummary(): DMNTable = dmnTableSummary
  def extendedRules(): List[ExtendedRule] = extendedRules
  def isLeafTable(): Boolean = isLeafTable

}

object ExtendedDMNTable {

  def apply(dmnTableSummary: DMNTable, context: DecisionDiagram, feelEngine: FeelEngine) = {
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
      new ExtendedDMNTable(dmnTableSummary, extendedRules, false)
    }
  }
}