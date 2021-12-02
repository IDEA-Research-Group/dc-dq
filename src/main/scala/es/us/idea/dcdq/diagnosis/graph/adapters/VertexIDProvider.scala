package es.us.idea.dcdq.diagnosis.graph.adapters

import es.us.idea.dcdq.diagnosis.graph.components.basic.Vertex
import org.jgrapht.ext.VertexNameProvider


class VertexIDProvider extends VertexNameProvider[Vertex] {
  override def getVertexName(vertex: Vertex): String = { //return vertex.toString().split("@")[1].split("\\[")[0];
    "A" + vertex.toString.split("@")(1).split("\\[")(0)
  }
}
