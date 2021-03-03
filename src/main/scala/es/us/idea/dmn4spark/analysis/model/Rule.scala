package es.us.idea.dmn4spark.analysis.model

import java.util.NoSuchElementException

import es.us.idea.dmn4spark.Combinations
import org.camunda.bpm.dmn.engine.impl.DmnDecisionTableRuleImpl
import org.camunda.bpm.dmn.feel.impl.FeelEngine

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

class Rule(order: Int, conditions: List[Condition], conclusions: List[Value]) {
  def conditions(): List[Condition] = conditions
  def conclusions(): List[Value] = conclusions
  def order(): Int = order

  def extendRule(values: List[Value], feelEngine: FeelEngine): List[ExtendedRule] = {

    // para cada condición, obtener la lista de de values validos
    val validInputValues = conditions.map(c => c.validInputValues(values, feelEngine))

    // generar todas las combinaciones posibles para todos los input values
    // Debe salir una lista de listas. Cada lista de Value contiene los values que hacen que la regla se dispare
    val allCombinations = Combinations.combinations(validInputValues)

    //TODO crear las ExtendedRules fusionandola con la conclusion
    allCombinations.map(r => ExtendedRule(order, r, conclusions))
  }

  override def toString: String = s"Rule{conditions: ${conditions().mkString(", ")}, conclusions: ${conclusions().mkString(", ")}"

}

object Rule {

  def apply(order: Int, rule: DmnDecisionTableRuleImpl, inputAttributes: List[Attribute], outputValues: List[Value]): Rule = {
    val conditions = rule.getConditions.asScala.zip(inputAttributes)
      .map(cond => Condition(cond._2, cond._1.getExpression)).toList
    val conclusions = rule.getConclusions.asScala
      .map(concl => {
        try {
          val outputValue = outputValues.find(ov => ov.value == concl.getExpression).get
          outputValue
        } catch {
          case nse: NoSuchElementException =>
            throw new NoSuchElementException(s"The output value with expression ${concl.getExpression} is not declared" +
              s"in the table output.")
        }
      }).toList
    new Rule(order, conditions, conclusions)
  }

}
