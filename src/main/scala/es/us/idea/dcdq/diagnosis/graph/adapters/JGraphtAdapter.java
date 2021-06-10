package es.us.idea.dcdq.diagnosis.graph.adapters;

import es.us.idea.dcdq.diagnosis.graph.components.basic.DirectedEdge;
import es.us.idea.dcdq.diagnosis.graph.components.basic.Vertex;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;


import java.io.StringWriter;
import java.io.Writer;
import java.util.Set;

public class JGraphtAdapter {

    public static SimpleDirectedGraph<Vertex, DefaultEdge> generateGraph
            (Set<Vertex> vertices, Set<DirectedEdge> edges) {

        SimpleDirectedGraph graph = new SimpleDirectedGraph<Vertex, DefaultEdge>(DefaultEdge.class);

        for(Vertex v: vertices)
            graph.addVertex(v);

        for(DirectedEdge e: edges)
            graph.addEdge(e.source(), e.target());

        return graph;
    }

    public static String basicGraphToDot(SimpleDirectedGraph<Vertex, DefaultEdge> graph) {
        VertexIDProvider vidp = new VertexIDProvider();
        VertexLabelProvider vlp = new VertexLabelProvider();

        DOTExporter<Vertex, DefaultEdge> exporter = new DOTExporter<>(vidp, vlp, null);
        Writer writer = new StringWriter();
        exporter.exportGraph(graph, writer);
        return writer.toString();
    }

    @Deprecated
    public static String printDot(Set<Vertex> vertices, Set<DirectedEdge> edges) {

        SimpleDirectedGraph graph = generateGraph(vertices, edges);

        VertexIDProvider vidp = new VertexIDProvider();
        VertexLabelProvider vlp = new VertexLabelProvider();

        DOTExporter<Vertex, DefaultEdge> exporter = new DOTExporter<>(vidp, vlp, null);
        Writer writer = new StringWriter();
        exporter.exportGraph(graph, writer);
        return writer.toString();
    }


}
