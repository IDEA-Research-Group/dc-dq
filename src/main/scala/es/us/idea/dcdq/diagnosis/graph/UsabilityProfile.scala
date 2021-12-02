package es.us.idea.dcdq.diagnosis.graph

import es.us.idea.dcdq.diagnosis.graph.components.basic.{DirectedEdge, Vertex}
import es.us.idea.dcdq.diagnosis.graph.structure.DMN4DQStructure
import play.api.libs.json.JsObject

class UsabilityProfile(vertices: Set[Vertex], edges: Set[DirectedEdge], structureOpt: Option[DMN4DQStructure] = None)
  extends DMN4DQTree(vertices, edges, structureOpt) {

}

object UsabilityProfile {
  def deserializeJson(jsObject: JsObject): UsabilityProfile = UsabilityProfile(DMN4DQTree.deserializeJson(jsObject))

  def apply(vertices: Set[Vertex],
            edges: Set[DirectedEdge],
            structureOpt: Option[DMN4DQStructure] = None): UsabilityProfile =
    new UsabilityProfile(vertices, edges, structureOpt)

  def apply(dmn4dqTree: DMN4DQTree) =
    new UsabilityProfile(dmn4dqTree.vertices(), dmn4dqTree.edges(), dmn4dqTree.structureOpt())

  def apply(tree: Tree) = new UsabilityProfile(tree.vertices(), tree.edges())

}
