package es.us.idea.dcdq.diagnosis.graph.adapters

import es.us.idea.dcdq.diagnosis.graph.components.basic.DirectedEdge
import es.us.idea.dcdq.diagnosis.graph.components.basic.Vertex
import org.jgrapht.ext.DOTExporter
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleDirectedGraph

import java.io.StringWriter
import java.io.Writer
import java.util
import java.util.Set


object JGraphtAdapter {
  def generateGraph(vertices: util.Set[Vertex], edges: util.Set[DirectedEdge]): SimpleDirectedGraph[Vertex, DefaultEdge] = {
    val graph = new SimpleDirectedGraph[Vertex, DefaultEdge](classOf[DefaultEdge])
    import scala.collection.JavaConversions._
    for (v <- vertices) {
      graph.addVertex(v)
    }
    import scala.collection.JavaConversions._
    for (e <- edges) {
      graph.addEdge(e.source, e.target)
    }
    graph
  }

  def basicGraphToDot(graph: SimpleDirectedGraph[Vertex, DefaultEdge]): String = {
    val vidp = new VertexIDProvider
    val vlp = new VertexLabelProvider
    val exporter = new DOTExporter[Vertex, DefaultEdge](vidp, vlp, null)
    val writer = new StringWriter
    exporter.exportGraph(graph, writer)
    writer.toString
  }

  @deprecated def printDot(vertices: util.Set[Vertex], edges: util.Set[DirectedEdge]): String = {
    val graph = generateGraph(vertices, edges)
    val vidp = new VertexIDProvider
    val vlp = new VertexLabelProvider
    val exporter = new DOTExporter[Vertex, DefaultEdge](vidp, vlp, null)
    val writer = new StringWriter
    exporter.exportGraph(graph, writer)
    writer.toString
  }
}