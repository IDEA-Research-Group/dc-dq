package es.us.idea.dcdq.diagnosis.graph.adapters;

import es.us.idea.dcdq.diagnosis.graph.components.basic.Vertex;
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
