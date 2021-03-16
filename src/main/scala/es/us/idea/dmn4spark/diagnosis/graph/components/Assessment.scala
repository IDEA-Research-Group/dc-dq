package es.us.idea.dmn4spark.diagnosis.graph.components

import es.us.idea.dmn4spark.diagnosis.graph.DMN4DQTree
import es.us.idea.dmn4spark.diagnosis.graph.components.basic.Vertex

class Assessment(value: String) extends Vertex {
  def value(): String = value

  override def toString: String = s"Assessment@$id[value=$value]"

  override def id(): String = value.hashCode.toHexString
}

object Assessment {

  def apply(value: String): Assessment = new Assessment(value)


}