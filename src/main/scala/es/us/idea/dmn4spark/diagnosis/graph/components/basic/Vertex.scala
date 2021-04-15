package es.us.idea.dmn4spark.diagnosis.graph.components.basic

import es.us.idea.dmn4spark.diagnosis.graph.{DMN4DQTree, Tree}
import play.api.libs.json.JsObject

trait Vertex extends Serializable {

  def id(): String

  def getChildren(implicit tree: Tree): Set[Vertex] = tree.getChildren(this)
  def getParents(implicit tree: Tree): Set[Vertex] = tree.getParents(this)
  def getAllDescendants(implicit tree: Tree): Set[Vertex] = tree.getAllDescendants(this)
  def isLeaf(implicit tree: Tree): Boolean = tree.isLeaf(this)

  def canEqual(a: Any): Boolean = a.isInstanceOf[Vertex]

  override def equals(that: Any): Boolean = {
    that match {
      case that: Vertex => that.canEqual(this) && that.id() == this.id()
      case _ => false
    }
  }

  override def hashCode(): Int = this.id().hashCode * 17

  def convert2json: JsObject

}
