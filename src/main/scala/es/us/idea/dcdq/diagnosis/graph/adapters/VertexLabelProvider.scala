package es.us.idea.dcdq.diagnosis.graph.adapters

import es.us.idea.dcdq.diagnosis.graph.components.basic.Vertex
import org.jgrapht.ext.VertexNameProvider

class VertexLabelProvider extends VertexNameProvider[Vertex] {
  override def getVertexName(vertex: Vertex): String = {
    val toString = vertex.toString
    val `type` = toString.split("@")(0)
    val value = toString.split("\\[")(1).replace("]", "")
    `type` + "\n" + value
  }
}
