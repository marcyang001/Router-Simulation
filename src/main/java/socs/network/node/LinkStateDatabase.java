package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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



	public RouterDescription getRd() {
		return this.rd;
	}
	
	/**
	 * delete every nodes that cannot be reached by the router
	 * 
	 * **/
	public void clean() {
		LinkedList<String> list = new LinkedList<String>();
		System.out.println("HOST:" + rd.simulatedIPAddress);
		synchronized(_store) {
			for (String ip: _store.keySet()) {
				list.add(ip);
			}
			for (int i = list.size()-1; i>=0; i--) {
				if (!list.get(i).equals(rd.simulatedIPAddress)) {
					System.out.println(list.get(i));
					System.out.println(getShortestPath(list.get(i)));
					if (getShortestPath(list.get(i)) == null) {
						_store.remove(list.get(i));
					}
				}
			}
		}
	}

	public void updateLSA(String ipAddress, LSA lsa) {
		synchronized(_store) {
			_store.put(ipAddress, lsa);
		}
	}
	
	public void removeLSA(String ipAddress) {
		synchronized(_store) {
			_store.remove(ipAddress);
		}
	}
	
	
	//delete the lost neighbor everywhere in the database
	public boolean deleteNeighbor(String ipAddress) {
		boolean neighborDeleted = false;
		synchronized(_store) {
			//delete the rest of the components in of the lost neighbor in the database
			for (LSA lsa : _store.values()) {
				List<LinkDescription> list = Collections.synchronizedList(lsa.links);
				synchronized (list) {
					for (LinkDescription l : list) {
						if (l.linkID.equals(ipAddress)) {
							list.remove(l);
							lsa.lsaSeqNumber++;
							neighborDeleted = true;
						}
					}
				}
			}
			return neighborDeleted;
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
	
	/**Clean everything in the database **/
	public void cleanAll() {
		// TODO Auto-generated method stub
		LinkedList<String> list = new LinkedList<String>();
		
		synchronized(_store) {
			for (String ip: _store.keySet()) {
				list.add(ip);
			}
			for (int i = list.size()-1; i>=0; i--) {
				if (!list.get(i).equals(rd.simulatedIPAddress)) {
						_store.remove(list.get(i));
				}
			}
		}
		
	}

}//end of the class 
