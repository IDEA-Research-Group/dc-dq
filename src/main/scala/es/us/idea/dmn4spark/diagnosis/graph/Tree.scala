package es.us.idea.dmn4spark.diagnosis.graph

import es.us.idea.dmn4spark.diagnosis.graph.components.basic.{DirectedEdge, Vertex}

trait Tree{
  def vertices(): List[Vertex]
  def edges(): List[DirectedEdge]
  def getChildren(vertex: Vertex): List[Vertex]
  def isLeaf(vertex: Vertex): Boolean
  def getAllDescendants(vertex: Vertex): List[Vertex]
  def getSubTree(vertex: Vertex): Tree
}
