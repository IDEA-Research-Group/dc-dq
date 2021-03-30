package es.us.idea.dmn4spark.diagnosis.graph

import es.us.idea.dmn4spark.analysis.Utils
import es.us.idea.dmn4spark.diagnosis.graph.components.basic.{AndVertex, DirectedEdge, Vertex}

class Tree(vertices: List[Vertex], edges: List[DirectedEdge]) extends Serializable {

  implicit val __ : Tree = this
  lazy val id: String = hashCode().toHexString


  def vertices(): List[Vertex] = vertices
  def edges(): List[DirectedEdge] = edges

  def getChildren(vertex: Vertex): List[Vertex] =
    edges().filter(_.source() == vertex).map(_.target())

  def getParents(vertex: Vertex): List[Vertex] =
    edges().filter(_.target() == vertex).map(_.source())

  def getRoots(): List[Vertex] = {
    val allTargetVertices = edges().map(_.target()).distinct
    vertices().filterNot(vertex => allTargetVertices.contains(vertex))
  }

  def pruneDescendants(vertex: Vertex): Tree = {
    val descendants = vertex.getAllDescendants
    Tree(
      vertices().filter(!descendants.contains(_)),
      edges().filter(edge => !descendants.contains(edge.source()) && !descendants.contains(edge.target()))
    )
  }

  def pruneDescendants(vertices: List[Vertex]): Tree = {
     //.reduce((t1, t2) => t1.union(t2))
    union(vertices.map(pruneDescendants))
  }

  def isLeaf(vertex: Vertex): Boolean =
    !edges.exists(_.source() == vertex)

  def getAllDescendants(vertex: Vertex): List[Vertex] = {
    val children = vertex.getChildren
    // get children of children
    children ::: children.flatMap(getAllDescendants)
  }

  def findAllBranches(vertex: Vertex): List[Tree] = {
    def recursive(v: Vertex, visited: List[Vertex], visitedEdges: List[DirectedEdge]): List[Tree] = {
      var toReturn: List[Tree] = List()
      v.getChildren.foreach(child =>{
        if(child.isLeaf) toReturn = toReturn :+ Tree(visited :+ child, visitedEdges :+ DirectedEdge(v, child))
        else toReturn = toReturn ++ recursive(child, visited:+child, visitedEdges :+ DirectedEdge(v, child))
      })
      toReturn
    }
    recursive(vertex, List(vertex), List())
  }

  def findAllBranchesAndOrSensitive(vertex: Vertex): List[Tree] = {

    def recursive(v: Vertex, visited: List[Vertex], visitedEdges: List[DirectedEdge]): List[Tree] = {
      var toReturn: List[Tree] = List()

      v.getChildren.foreach(child =>{
        if(child.isLeaf) toReturn = toReturn :+ Tree(visited :+ child, visitedEdges :+ DirectedEdge(v, child))
        else {
          child match {
            case andVertex: AndVertex => toReturn = toReturn ++ recursiveAnd(andVertex, visited:+andVertex, visitedEdges :+ DirectedEdge(v, andVertex))
            case _ => toReturn = toReturn ++ recursive(child, visited:+child, visitedEdges :+ DirectedEdge(v, child))
          }
        }
      })
      toReturn
    }

    def recursiveAnd(v: Vertex, visited: List[Vertex], visitedEdges: List[DirectedEdge]): List[Tree] = {
      var andVerticesBranches: List[(Vertex, Tree)] = List()

      v.getChildren.foreach(child => {
        if(child.isLeaf) andVerticesBranches = andVerticesBranches :+ (child, Tree(visited :+ child, visitedEdges :+ DirectedEdge(v, child)))
        else {
          child match {
            case andVertex: AndVertex =>
              andVerticesBranches = andVerticesBranches ++
                recursiveAnd(andVertex, visited :+ andVertex, visitedEdges :+ DirectedEdge(v, andVertex)).map(x => (andVertex, x))
            case _ =>
              andVerticesBranches = andVerticesBranches ++
                recursive(child, visited :+ child, visitedEdges :+ DirectedEdge(v, child)).map(x => (child, x))
          }
        }
      })

      val groupedBranches = andVerticesBranches.groupBy(_._1).map(x => x._2.map(_._2)).toList
      Utils.combinations(groupedBranches).map(el => union(el))
    }
    recursive(vertex, List(vertex), List())
  }

  def union(trees: List[Tree]): Tree = {
    val (vertices, edges) = trees.map(tree => (tree.vertices.toSet, tree.edges.toSet)).reduce((x, y) => (x._1 union y._1, x._2 union y._2))
    Tree(vertices.toList, edges.toList)
  }

//  def union(tree: Tree): Tree = {
//    val (vertices, edges) = (tree.vertices().toSet union this.vertices().toSet, tree.edges().toSet union this.edges().toSet)
//    Tree(vertices.toList, edges.toList)
//  }

  def canEqual(a: Any): Boolean = a.isInstanceOf[Tree]

  override def equals(that: Any): Boolean = {
    that match {
      case that: Tree => {
        that.canEqual(this) &&
          that.vertices().toSet.equals(this.vertices().toSet) &&
          that.edges().toSet.equals(this.edges().toSet)
      }
      case _ => false
    }
  }

  override def hashCode(): Int = vertices().toSet.hashCode() * edges().toSet.hashCode() * 47

  def getId: String = id

}

object Tree {
  def apply(vertices: List[Vertex], edges: List[DirectedEdge]): Tree = new Tree(vertices, edges)
}