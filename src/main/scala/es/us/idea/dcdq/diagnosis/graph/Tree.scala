package es.us.idea.dcdq.diagnosis.graph

import es.us.idea.dcdq.analysis.Utils
import es.us.idea.dcdq.diagnosis.graph.adapters.JGraphtAdapter
import es.us.idea.dcdq.diagnosis.graph.components.basic.{AndVertex, DirectedEdge, Vertex}
import org.jgrapht.graph.{DefaultEdge, SimpleDirectedGraph}
import play.api.libs.json.{JsArray, JsObject, JsString}

import scala.collection.JavaConverters.{seqAsJavaListConverter, setAsJavaSetConverter}



class Tree(vertices: Set[Vertex], edges: Set[DirectedEdge]) extends Serializable {

  implicit val __ : Tree = this
  lazy val id: String = hashCode().toHexString


  def vertices(): Set[Vertex] = vertices
  def edges(): Set[DirectedEdge] = edges

  def verticesAndEdges(): (Set[Vertex], Set[DirectedEdge]) = (vertices(), edges())

  def getChildren(vertex: Vertex): Set[Vertex] =
    edges().filter(_.source() == vertex).map(_.target())

  def getParents(vertex: Vertex): Set[Vertex] =
    edges().filter(_.target() == vertex).map(_.source())

  def getRoots(): Set[_ <: Vertex] = {
    val allTargetVertices = edges().map(_.target())
    vertices().filterNot(vertex => allTargetVertices.contains(vertex))
  }

//  // FIXME prune methods
//  def pruneDescendants(vertex: Vertex): Tree = {
//    val descendants = vertex.getAllDescendants
//    Tree(
//      vertices().filter(!descendants.contains(_)),
//      edges().filter(edge => !descendants.contains(edge.source()) && !descendants.contains(edge.target()))
//    )
//  }
//
//  def pruneDescendants(vertices: List[Vertex]): Tree = {
//     //.reduce((t1, t2) => t1.union(t2))
//    union(vertices.map(pruneDescendants))
//  }

  def isLeaf(vertex: Vertex): Boolean =
    !edges.exists(_.source() == vertex)

  def isRoot(vertex: Vertex): Boolean =
    !edges.exists(_.target() == vertex)

  def getAllDescendants(vertex: Vertex): Set[Vertex] = {
    val children = vertex.getChildren
    // get children of children
    //children ::: children.flatMap(getAllDescendants)
    children ++ children.flatMap(getAllDescendants)
  }

//  def findAllBranches(vertex: Vertex): List[Tree] = {
//    def recursive(v: Vertex, visited: Set[Vertex], visitedEdges: Set[DirectedEdge]): List[Tree] = {
//      var toReturn: List[Tree] = List()
//      v.getChildren.foreach(child =>{
//        if(child.isLeaf) toReturn = toReturn :+ Tree(visited + child, visitedEdges + DirectedEdge(v, child))
//        else toReturn = toReturn ++ recursive(child, visited+child, visitedEdges + DirectedEdge(v, child))
//      })
//      toReturn
//    }
//    recursive(vertex, Set(vertex), Set())
//  }

  def allPathsFromVertex(
                          from: Vertex,
                          baseCase: Vertex => Boolean = this.isLeaf,
                          nextGeneration: Vertex => Set[Vertex] = this.getChildren,
                          visited: List[Vertex] = List()
                        ): List[List[Vertex]] = {
    var result: List[List[Vertex]] = List()
    nextGeneration(from).foreach(next => {
      if(baseCase(next)) result = result :+ (visited :+ next)
      else result = result ++ allPathsFromVertex(next, baseCase, nextGeneration, visited :+ next)
    })
    result
  }

  def allPathsToVertex (
                          to: Vertex,
                          baseCase: Vertex => Boolean = this.isRoot,
                          visited: List[Vertex] = List()
                        ): List[List[Vertex]] = allPathsFromVertex(to, baseCase, this.getParents, visited)


  def union(trees: List[Tree]): Tree = {
    val (vertices, edges) = trees.map(tree => (tree.vertices, tree.edges)).reduce((x, y) => (x._1 union y._1, x._2 union y._2))
    Tree(vertices, edges)
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

  def convert2json: JsObject = {
    JsObject(Seq(
      "id" -> JsString(getId),
      "vertices" -> JsArray(vertices().toList.map(_.convert2json)),
      "edges" -> JsArray(edges().toList.map(_.convert2json))
    ))
  }

  def asJgraphT: SimpleDirectedGraph[Vertex, DefaultEdge] =
    JGraphtAdapter.generateGraph(vertices().asJava, edges().asJava)

  def dotRepresentation: String = JGraphtAdapter.basicGraphToDot(this.asJgraphT)

}

object Tree {
  def apply(vertices: Set[Vertex], edges: Set[DirectedEdge]): Tree = new Tree(vertices, edges)
}