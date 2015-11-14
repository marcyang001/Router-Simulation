package socs.network.node;

/**this is a child thread that spawns off upon start(), 
 * it sends HELLO messages periodically to neighbors to see if the neighbor is active
 * 
 * sends HELLO every 5 seconds
 *  
 *  **/ 

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;

public class SignalMessage implements Runnable {
	
	protected Timer timer;
	PeriodMessage pm;
	Socket client = null;
	Router router;
	
	public SignalMessage(Link trackingLink, Socket socket, Router router) {
		// TODO Auto-generated constructor stub
		this.router = router;
		timer = new Timer();
		pm = new PeriodMessage(trackingLink, socket, router);

	}
	
	




	public void run() {
		// TODO Auto-generated method stub
		//send HELLO to neighbors every 5 seconds
		timer.scheduleAtFixedRate(pm, new Date(), 5000);
		
	}
	

class PeriodMessage extends TimerTask {
	
//	Socket[] clients = new Socket[4];
	Socket sender;
	Link neighbor;
	Router m_router;
	

	public PeriodMessage(Link lostlink, Socket socket, Router router) {
		// TODO Auto-generated constructor stub
		this.m_router = router;
		this.sender = socket;
		this.neighbor = lostlink;
		
	}
	

	public void run() {
		// TODO Auto-generated method stub
		
		//System.out.println("PERIODICALLY SENT MESSAGES");
		ObjectOutputStream sendHello;
		ObjectInputStream inStream;
		try {
			sendHello = new ObjectOutputStream(sender.getOutputStream());
			SOSPFPacket responsePacket = new SOSPFPacket(
					m_router.rd.processIPAddress,
					m_router.rd.processPortNumber,
					m_router.rd.simulatedIPAddress,
					neighbor.router2.simulatedIPAddress,
					(short) 0, m_router.rd.simulatedIPAddress,
					m_router.rd.simulatedIPAddress, (short)-1);
			//sends the hello message periodically
			sendHello.writeObject(responsePacket);
			
			//receive the input 
			inStream = new ObjectInputStream(sender.getInputStream());
			SOSPFPacket packetFromClient = (SOSPFPacket) inStream.readObject();
			if (packetFromClient.sospfType == 0) {
				//System.out.println("received HELLO from " + packetFromClient.neighborID + "; ");
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			/**
			 * catch the lost neighbor:
			 * 1. stop the timer 
			 * 2. delete the neighbor from the port + potential neighbor
			 * 3. delete the neighbor from the database
			 * 4. delete the neighbor link from the current router LSA
			 * 5. increment the sequence number for update
			 * 6. update its own database
			 * 7. send the updated LSA to the rest of the neighbors
			 * 8. kill the current thread
			 *  **/
			//1.
			timer.cancel();
			System.out.println("NEIGHBOR IS LOST!!!!!");
			
			//2 
			List<Link> list = new ArrayList<Link>(Arrays.asList(m_router.ports));
			List<Link> listPot = new ArrayList<Link>(Arrays.asList(m_router.potentialNeighbors));
			for (int i = 0; i<m_router.ports.length; i++) {
				if (m_router.ports[i] != null) {
						if (m_router.ports[i].router2.simulatedIPAddress.equals(neighbor.router2.simulatedIPAddress)) {
							System.out.println("REMOVE THE LINK FROM PORT!!!!!!");
							list.removeAll(Arrays.asList(m_router.ports[i]));
							m_router.ports = list.toArray(m_router.ports);
							for (int j = 0; j < m_router.potentialNeighbors.length; j++) {
								if (m_router.potentialNeighbors[j] != null) {
									if (m_router.potentialNeighbors[j].router2.simulatedIPAddress.equals(neighbor.router2.simulatedIPAddress)) {
										System.out.println("REMOVE THE LINK FROM POTENTIAL NEIGHBORS!!!!!!");
										listPot.removeAll(Arrays.asList(m_router.potentialNeighbors[j]));
										m_router.potentialNeighbors = listPot.toArray(m_router.potentialNeighbors);
										break;
									}
								}
							}
							break;
						}
				}
			}
			
			
			
			//3. 
			m_router.lsd.removeLSA(neighbor.router2.simulatedIPAddress);
			//4.
			LinkDescription lostLink = new LinkDescription();
			lostLink.linkID = neighbor.router2.simulatedIPAddress;
			lostLink.portNum = neighbor.router2.processPortNumber;
			lostLink.tosMetrics = neighbor.weight;
			
			
			for (int i = 0; i< this.m_router.lsa.links.size(); i++) {
				
				if (this.m_router.lsa.links.get(i).linkID.equals(lostLink.linkID)) {
					this.m_router.lsa.links.remove(i);
					//5.
					this.m_router.lsa.lsaSeqNumber++;
					System.out.println("LINK DELETED FROM LSA");
					break;
				}
			}
			//6
			m_router.lsd.updateLSA(m_router.rd.simulatedIPAddress, m_router.lsa);
			m_router.lsd.deleteNeighbor(neighbor.router2.simulatedIPAddress);
			
			SOSPFPacket responsePacket = new SOSPFPacket(
					m_router.rd.processIPAddress,
					m_router.rd.processPortNumber,
					m_router.rd.simulatedIPAddress,
					neighbor.router2.simulatedIPAddress,
					(short) 1, m_router.rd.simulatedIPAddress,
					m_router.rd.simulatedIPAddress, (short)-1);
			
			responsePacket.lsaArray.add(m_router.lsa);
			responsePacket.originalSender = neighbor.router2.simulatedIPAddress;
			
			SOSPFPacket newPacket = m_router.generateFullPacketUpdate((short)4, responsePacket);
			
			//7.
			m_router.broadcastToNeighbors(neighbor.router2.simulatedIPAddress, newPacket, (short)4);
			
			
			System.out.println(m_router.lsd.toString());
			
			//8.
			Thread.currentThread().interrupt();
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			timer.cancel();
			e.printStackTrace();
		}		
	}
	
}//end of PeriodMessage TimerTask
	
	
	

}// ending of SignalMessage class





