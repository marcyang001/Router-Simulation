package socs.network.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;


public class SignalMessageServer implements Runnable{

	protected Timer timer;
	Socket client = null;
	ServerInputOutput serverRouter;
	PingTask pt;
	
	
	public SignalMessageServer(Link trackingLink, ServerInputOutput serverInputOutput) {
		// TODO Auto-generated constructor stub
		
		this.serverRouter = serverInputOutput;
		this.timer = new Timer();
		try {
			this.client = new Socket(trackingLink.router2.processIPAddress, trackingLink.router2.processPortNumber);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("FAIL TO CONNECT BACK TO THE CLIENT");
		}
		pt = new PingTask(trackingLink, client, serverInputOutput);
		
	}
	
	public void run() {
		// TODO Auto-generated method stub
		timer.scheduleAtFixedRate(pt, new Date(), 5000);
	}
	
	class PingTask extends TimerTask {
		
		Socket sender;
		Link neighbor;
		ServerInputOutput m_server;

		public PingTask(Link trackingLink, Socket client, ServerInputOutput serverInputOutput) {
			// TODO Auto-generated constructor stub
			this.m_server = serverInputOutput;
			this.sender = client;
			this.neighbor = trackingLink;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			//System.out.println("PERIODICALLY SENT MESSAGES");
			ObjectOutputStream sendHello;
			ObjectInputStream inStream;
			try {
				sendHello = new ObjectOutputStream(sender.getOutputStream());
				SOSPFPacket responsePacket = new SOSPFPacket(
						m_server.serverRouter.processIPAddress,
						m_server.serverRouter.processPortNumber,
						m_server.serverRouter.simulatedIPAddress,
						neighbor.router2.simulatedIPAddress,
						(short) 10, m_server.serverRouter.simulatedIPAddress,
						m_server.serverRouter.simulatedIPAddress, (short)-1);
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
				System.out.println("NEIGHBOR IS LOST (FROM SERVER)!!!!!");
				
				//2 
				List<Link> list = new ArrayList<Link>(Arrays.asList(m_server.mm_ports));
				List<Link> listPot = new ArrayList<Link>(Arrays.asList(m_server.mm_potentialNeighbors));
				for (int i = 0; i<m_server.mm_ports.length; i++) {
					if (m_server.mm_ports[i] != null) {
						if (m_server.mm_ports[i].router2.simulatedIPAddress.equals(neighbor.router2.simulatedIPAddress)) {
							System.out.println("REMOVE THE LINK FROM PORT!!!!!!");
							list.removeAll(Arrays.asList(m_server.mm_ports[i]));
							m_server.mm_ports = list.toArray(m_server.mm_ports);
							for (int j = 0; j < m_server.mm_potentialNeighbors.length; j++) {
								if (m_server.mm_potentialNeighbors[j] != null) {
									if (m_server.mm_potentialNeighbors[j].router2.simulatedIPAddress.equals(neighbor.router2.simulatedIPAddress)) {
										System.out.println("REMOVE THE LINK FROM POTENTIAL NEIGHBORS!!!!!!");
										listPot.removeAll(Arrays.asList(m_server.mm_potentialNeighbors[j]));
										m_server.mm_potentialNeighbors = listPot.toArray(m_server.mm_potentialNeighbors);
										break;
									}
								}
							}
							break;
						}
					}
				}
				
				//remove the link from LSA, but dont increment seq number because it will be incremented 
				//in neighborDelete in database
				for (LinkDescription l: m_server.mm_lsa.links) {
					if(l.linkID.equals(neighbor.router2.simulatedIPAddress)) {
						m_server.mm_lsa.links.remove(l);
						m_server.mm_lsa.lsaSeqNumber++;
						break;
					}
				}
				
				m_server.mm_database.updateLSA(m_server.serverRouter.simulatedIPAddress, m_server.mm_lsa);
				
				m_server.mm_database.removeLSA(neighbor.router2.simulatedIPAddress);
				
				m_server.mm_database.deleteNeighbor(neighbor.router2.simulatedIPAddress);
				
				//cleaning : remove all the nodes that cannot be reached
				//m_server.mm_database.clean();
				
				
				
				if (m_server.mm_ports[0] == null && m_server.mm_potentialNeighbors[0]== null) {
					m_server.mm_database.cleanAll();
				}
				
				//System.out.println("NOW: " + m_server.mm_lsa.lsaSeqNumber);
				
				SOSPFPacket responsePacket = new SOSPFPacket(
						m_server.serverRouter.processIPAddress,
						m_server.serverRouter.processPortNumber,
						m_server.serverRouter.simulatedIPAddress,
						neighbor.router2.simulatedIPAddress,
						(short) 1, m_server.serverRouter.simulatedIPAddress,
						m_server.serverRouter.simulatedIPAddress, (short)-1);
				
				responsePacket.lsaArray.add(m_server.mm_lsa);
				responsePacket.originalSender = neighbor.router2.simulatedIPAddress;
				
				
				//7.
				m_server.broadcastToNeighbors(neighbor.router2.simulatedIPAddress, responsePacket, (short)4);
				
				//System.out.println("THEN: " + m_server.mm_lsa.lsaSeqNumber);
				System.out.println(m_server.mm_database.toString());
				
				//8.
				Thread.currentThread().interrupt();
				
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				timer.cancel();
				e.printStackTrace();
			}
			
		}
		
		
		
	}

	
	
	
	
	

}
