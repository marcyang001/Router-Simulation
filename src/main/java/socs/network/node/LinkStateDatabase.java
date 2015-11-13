package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

public class LinkStateDatabase implements Serializable {

	

	// linkID => LSAInstance
	HashMap<String, LSA> _store = new HashMap<String, LSA>();

	transient private RouterDescription rd = null;

	public LinkStateDatabase(RouterDescription routerDescription) {
		rd = routerDescription;
		LSA l = initLinkStateDatabase();
		_store.put(l.linkStateID, l);
	}

	/**
	 * output the shortest path from this router to the destination with the
	 * given IP address
	 */
	String getShortestPath(String destinationIP) {
		synchronized(_store) {
		ArrayList<String> nodes = new ArrayList<String>();
		ArrayList<Edge> edges = new ArrayList<Edge>();
		for (String key : _store.keySet()) {
			nodes.add(key);
			for (LinkDescription ld : _store.get(key).links) {
				Edge e = new Edge(key, ld.linkID, ld.tosMetrics);
				edges.add(e);
			}
		}
		Dijkstra dijkstra = new Dijkstra(nodes, edges);
	    dijkstra.execute(rd.simulatedIPAddress);
	    return dijkstra.getPath(destinationIP);
		}
	}



	public RouterDescription getRd() {
		return this.rd;
	}

	public void updateLSA(String ipAddress, LSA lsa) {
		synchronized(_store) {
			if (_store.containsKey(ipAddress)) {
				System.out.println("UPDATED HERE!!!!!ASDHAHS");
				_store.remove(ipAddress);
				_store.put(ipAddress, lsa);
			}
			else {
				_store.put(ipAddress, lsa);
			}
			
		}
	}
	
	public void removeLSA(String ipAddress) {
		synchronized(_store) {
			_store.remove(ipAddress);
		}
	}
	
	public void deleteNeighbor(String ipAddress) {
		synchronized(_store) {
			//delete the rest of the components in of the lost neighbor in the database
			for (LSA lsaInDatabase : _store.values()) {
				for (LinkDescription linkInDatabase : lsaInDatabase.links) {
					if (linkInDatabase.linkID.equals(ipAddress)) {
						lsaInDatabase.links.remove(linkInDatabase);
					}
				}
			}
		}
	}
	

	// initialize the linkstate database by adding an entry about the router
	// itself
	private LSA initLinkStateDatabase() {
		LSA lsa = new LSA();
		lsa.linkStateID = rd.simulatedIPAddress;
		lsa.lsaSeqNumber = Integer.MIN_VALUE;
		LinkDescription ld = new LinkDescription();
		ld.linkID = rd.simulatedIPAddress;
		ld.portNum = -1;
		ld.tosMetrics = 0;
		lsa.links.add(ld);
		return lsa;
	}

	public Vector<LSA> retrieveLSAs() {
		Vector<LSA> lsa = new Vector<LSA>();
		synchronized(_store) {
			for (String key : _store.keySet()) {
				lsa.add(_store.get(key));
			}
			return lsa;
		}

	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (LSA lsa : _store.values()) {
			sb.append(lsa.linkStateID).append("(" + lsa.lsaSeqNumber + ")")
					.append(":\t");
			for (LinkDescription ld : lsa.links) {
				sb.append(ld.linkID).append(",").append(ld.portNum).append(",")
						.append(ld.tosMetrics).append("\t");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

}
