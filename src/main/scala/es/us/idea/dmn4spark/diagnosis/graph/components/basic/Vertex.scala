package es.us.idea.dmn4spark.diagnosis.graph.components.basic

import es.us.idea.dmn4spark.diagnosis.graph.{DMN4DQTree, Tree}

trait Vertex {

  def id(): String

  def getChildren(implicit tree: Tree): List[Vertex] = tree.getChildren(this)
  def getParents(implicit tree: Tree): List[Vertex] = tree.getParents(this)
  def getAllDescendants(implicit tree: Tree): List[Vertex] = tree.getAllDescendants(this)
  def isLeaf(implicit tree: Tree): Boolean = tree.isLeaf(this)

  def canEqual(a: Any): Boolean = a.isInstanceOf[Vertex]

  override def equals(that: Any): Boolean = {
    that match {
      case that: Vertex => that.canEqual(this) && that.id() == this.id()
      case _ => false
    }
  }

  override def hashCode(): Int = this.id().hashCode * 17


}
