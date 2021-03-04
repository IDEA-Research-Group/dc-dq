package es.us.idea.dmn4spark.analysis

import es.us.idea.dmn4spark.analysis.model.{Attribute, Value}
import org.camunda.bpm.dmn.engine.DmnDecision
import org.camunda.bpm.dmn.engine.impl.{DmnDecisionTableImpl, DmnDecisionTableRuleImpl}

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

class DMNTable(name: String, key: String, inputAttributes: List[Attribute], outputValues: List[Value],
               rules: List[Rule]) {

  // getters
  def name(): String = name
  def key(): String = key
  def inputAttributes(): List[Attribute] = inputAttributes
  def outputValues(): List[Value] = outputValues
  def rules(): List[Rule] = rules



  override def toString: String = s"DMNTableSummary{name: $name, key: $key, " +
    s"inputAttributes: [${inputAttributes.mkString(", ")}], outputValues: [${outputValues.mkString(", ")}], " +
    s"#rules: ${rules.size}, rules: ${rules.mkString(", ")}"

}

object DMNTable {
  def apply(dmnDecision: DmnDecision): DMNTable = {
    val name = dmnDecision.getName
    val key = dmnDecision.getKey

    val dmnDecisionTable = dmnDecision.getDecisionLogic.asInstanceOf[DmnDecisionTableImpl]
    val nativeRules = dmnDecisionTable.getRules.asScala.toList

    val inputAttributes = dmnDecisionTable.getInputs.asScala
      .map(x => Attribute(x.getExpression.getExpression, x.getExpression.getTypeDefinition.getTypeName)).toList

    // (name, datatype)
    val outputNamesAndType = dmnDecisionTable.getOutputs.asScala.map(x => (x.getOutputName, x.getTypeDefinition.getTypeName))

    // Result: (expression value)
    val outputExpressions = nativeRules.map(x => x.getConclusions.asScala.map(_.getExpression)).toList

    val outputValues = outputExpressions.flatMap(x => outputNamesAndType.zip(x).map(y => {
      val attName = y._1._1
      val attType = y._1._2
      val value = y._2
      Value(attName, value, attType)
    }))

    val rules = nativeRules.zipWithIndex.map(r => Rule(r._2, r._1, inputAttributes, outputValues))

    new DMNTable(name, key, inputAttributes, outputValues, rules)
  }
}