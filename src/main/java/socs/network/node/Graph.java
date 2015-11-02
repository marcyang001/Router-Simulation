package socs.network.node;

import java.util.ArrayList;

public class Graph {
  private final ArrayList<String> vertexes;
  private final ArrayList<Edge> edges;

  public Graph(ArrayList<String> vertexes, ArrayList<Edge> edges) {
    this.vertexes = vertexes;
    this.edges = edges;
  }

  public ArrayList<String> getVertexes() {
    return vertexes;
  }

  public ArrayList<Edge> getEdges() {
    return edges;
  }
} 
