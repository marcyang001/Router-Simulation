package socs.network.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Dijkstra {

	private final List<String> nodes;
	private final List<Edge> edges;
	private Set<String> nodesInC;
	private Set<String> nodesOutsideC;
	private HashMap<String, String> predecessors;
	private HashMap<String, Integer> distance;

	public Dijkstra(Graph graph) {
		// create a copy of the array so that we can operate on this array
		this.nodes = new ArrayList<String>(graph.getVertexes());
		this.edges = new ArrayList<Edge>(graph.getEdges());
	}

	public void execute(String source) {
		nodesInC = new HashSet<String>();
		nodesOutsideC = new HashSet<String>();
		distance = new HashMap<String, Integer>();
		predecessors = new HashMap<String, String>();
		distance.put(source, 0);
		// this is the C.
		nodesOutsideC.add(source);
		// Loop N-1 times
		while (nodesOutsideC.size() > 0) {
			String node = getMinimum(nodesOutsideC);
			nodesInC.add(node);
			nodesOutsideC.remove(node);
			findMinimalDistances(node);
		}
	}

	/*
	 * This method returns the path from the source to the selected target and
	 * NULL if no path exists
	 */
	public String getPath(String target) {
		LinkedList<String> path = new LinkedList<String>();
		String step = target;
		// check if a path exists
		if (predecessors.get(step) == null) {
			return null;
		}
		path.add(step);
		while (predecessors.get(step) != null) {
			step = predecessors.get(step);
			path.add(step);
		}
		// Put it into the correct order
		Collections.reverse(path);
		
		StringBuffer sb = new StringBuffer();
	    
		for(int i = 0; i<path.size(); i++) {
			if(i != path.size() -1) {
				sb.append(path.get(i) + " ->(" + getDistance(path.get(i), path.get(i+1) + ")" 
						+ " "));
			}else {
				sb.append(path.get(i));
			}
		}

	    return sb.toString();
	}
	
	private String getMinimum(Set<String> vertexes) {
		String minimum = null;
		for (String String : vertexes) {
			if (minimum == null) {
				minimum = String;
			} else {
				if (getShortestDistance(String) < getShortestDistance(minimum)) {
					minimum = String;
				}
			}
		}
		return minimum;
	}
	
	private void findMinimalDistances(String node) {

		ArrayList<String> adjacentNodes = getNeighbors(node);

		for (String target : adjacentNodes) {

			if (getShortestDistance(target) > getShortestDistance(node)
					+ getDistance(node, target)) {
				distance.put(target,
						getShortestDistance(node) + getDistance(node, target));
				predecessors.put(target, node);
				nodesOutsideC.add(target);
			}
		}

	}

	private ArrayList<String> getNeighbors(String node) {
		ArrayList<String> neighbors = new ArrayList<String>();
		for (Edge edge : edges) {
			if (edge.getSource().equals(node)
			// not in C
					&& !isOutsideC(edge.getDestination())) {
				neighbors.add(edge.getDestination());
			}
		}
		return neighbors;
	}

	private int getDistance(String node, String target) {
		for (Edge edge : edges) {
			if (edge.getSource().equals(node)
					&& edge.getDestination().equals(target) || 
					edge.getDestination().equals(node)
					&& edge.getSource().equals(target)) {
				return edge.getWeight();
			}
		}
		throw new RuntimeException("Should not happen");
	}

	private boolean isOutsideC(String String) {
		return nodesInC.contains(String);
	}

	private int getShortestDistance(String destination) {
		Integer d = distance.get(destination);
		if (d == null) {
			return Integer.MAX_VALUE;
		} else {
			return d;
		}
	}
}
