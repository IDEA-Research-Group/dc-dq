package es.us.idea.dmn4spark.analysis

import org.camunda.bpm.dmn.engine.DmnDecision
import org.camunda.bpm.dmn.engine.impl.DmnDecisionTableImpl
import scala.collection.JavaConverters.collectionAsScalaIterableConverter

case class Attribute(name: String, dataType: String)
case class Value(name: String, value: String, dataType: String)

class DMNTableSummary(name: String, key: String, inputAttributes: List[Attribute], outputValues: List[Value]) {

  // getters
  def name(): String = name
  def key(): String = key
  def inputAttributes(): List[Attribute] = inputAttributes
  def outputValues(): List[Value] = outputValues

  override def toString: String = s"DMNTableSummary{name: $name, key: $key, " +
    s"inputAttributes: [${inputAttributes.mkString(", ")}], outputValues: [${outputValues.mkString(", ")}]}"

}

object DMNTableSummary {
  def apply(dmnDecision: DmnDecision): DMNTableSummary = {
    val name = dmnDecision.getName
    val key = dmnDecision.getKey

    val dmnDecisionTable = dmnDecision.getDecisionLogic.asInstanceOf[DmnDecisionTableImpl]

    val inputAttributes = dmnDecisionTable.getInputs.asScala
      .map(x => Attribute(x.getExpression.getExpression, x.getExpression.getTypeDefinition.getTypeName)).toList

    // (name, datatype)
    val outputNamesAndType = dmnDecisionTable.getOutputs.asScala.map(x => (x.getOutputName, x.getTypeDefinition.getTypeName))

    // Result: (expression value)
    val outputExpressions = dmnDecisionTable.getRules.asScala.map(x => x.getConclusions.asScala.map(_.getExpression)).toList

    val outputValues = outputExpressions.flatMap(x => outputNamesAndType.zip(x).map(y => {
      val attName = y._1._1
      val attType = y._1._2
      val value = y._2
      Value(attName, value, attType)
    }))

    new DMNTableSummary(name, key, inputAttributes, outputValues)
  }
}