package es.us.idea.dcdq.analysis

import es.us.idea.dcdq.analysis.model.{Attribute, Value}
import org.camunda.bpm.dmn.feel.impl.FeelEngine
import org.camunda.bpm.engine.variable.impl.context.SingleVariableContext
import org.camunda.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl._

class Condition(inputAttribute: Attribute, expression: String) {
  def inputAttribute(): Attribute = inputAttribute
  def expression(): String = expression

  def validInputValues(values: List[Value], feelEngine: FeelEngine): List[Value] = {
    // First, filter those values which corresponds with the attribute which is evaluated in this condition
    val validValues = values.filter(value => value.name == inputAttribute.name)

    if(this.expression == null){
      validValues
    } else {
      validValues.filter(value => {
        val valueContent = value.value.replaceAll("^\"|\"$", "")
        // TODO move to external function
        val contextValue = value.dataType match {
          case "string" => new StringValueImpl(valueContent)
          case "boolean" => new BooleanValueImpl(valueContent.toBoolean)
          case "number" => new NumberValueImpl(valueContent.toDouble)
          case "double" => new DoubleValueImpl(valueContent.toDouble)
          case "integer" => new IntegerValueImpl(valueContent.toInt)
          case "long" => new LongValueImpl(valueContent.toLong)
          case _ => throw new Exception(s"Data type not supported. Error when analysing output datatype " +
            s"${value.dataType} with value $value.")
        }
        val context = SingleVariableContext.singleVariable(value.name, contextValue)
        feelEngine.evaluateSimpleUnaryTests(this.expression(), value.name, context)
      })
    }
  }

  override def toString: String = s"Condition{inputAttribute: $inputAttribute, expression: $expression}"
}

object Condition {
  def apply(inputAttribute: Attribute, expression: String): Condition = new Condition(inputAttribute, expression)
}