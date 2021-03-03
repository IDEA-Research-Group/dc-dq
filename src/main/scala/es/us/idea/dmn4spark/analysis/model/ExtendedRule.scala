package es.us.idea.dmn4spark.analysis.model

class ExtendedRule(order: Int, conditions: List[Value], conclusions: List[Value]) {

  def order():Int = order
  def conditions(): List[Value] = conditions
  def outputs(): List [Value] = conclusions

  override def toString: String = s"ExtendedRule{order: $order, conditions: ${conditions.mkString(", ")}, conclusions: ${conclusions.mkString(", ")}"

}

object ExtendedRule {

  def apply(order: Int, conditions: List[Value], conclusions: List[Value]): ExtendedRule = new ExtendedRule(order, conditions, conclusions)
  
}