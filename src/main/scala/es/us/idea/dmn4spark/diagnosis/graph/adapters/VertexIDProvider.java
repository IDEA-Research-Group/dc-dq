package es.us.idea.dmn4spark.diagnosis.graph.adapters;

import es.us.idea.dmn4spark.diagnosis.graph.components.basic.Vertex;
import org.jgrapht.ext.VertexNameProvider;

public class VertexIDProvider implements VertexNameProvider<Vertex> {

    @Override
    public String getVertexName(Vertex vertex) {
        //return vertex.toString().split("@")[1].split("\\[")[0];
        return "A"+vertex.toString().split("@")[1].split("\\[")[0];
    }
}
