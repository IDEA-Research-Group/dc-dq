package es.us.idea.dmn4spark.diagnosis.graph.adapters;

import es.us.idea.dmn4spark.diagnosis.graph.components.basic.Vertex;
import org.jgrapht.ext.VertexNameProvider;

public class VertexLabelProvider implements VertexNameProvider<Vertex> {

    @Override
    public String getVertexName(Vertex vertex) {
        String toString = vertex.toString();
        String type = toString.split("@")[0];
        String value = toString.split("\\[")[1].replace("]", "");

        return type+"\n"+value;
    }
}
