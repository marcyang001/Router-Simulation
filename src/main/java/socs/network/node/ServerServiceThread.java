package socs.network.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;

public class ServerServiceThread implements Runnable {
	ServerSocket sServer;
	Link[] m_ports;
	Link[] m_potentialNeighbors;
	ArrayList<SocketAddress> socketAddr = new ArrayList<SocketAddress>();
	SOSPFPacket packet;
	RouterDescription routerDesc;
	ServerInputOutput serverResponse;
	boolean flag = true;
	int portNum = 0;
	LinkStateDatabase m_lsd;
	LSA m_lsa;
	

	public ServerServiceThread(short portNum, RouterDescription rd,
			Link[] ports, Link[] potentialNeighbors, LinkStateDatabase lsd, LSA lsa) {
			
		try {
			this.m_ports = ports;
			this.m_potentialNeighbors = potentialNeighbors;
			sServer = new ServerSocket(portNum);
			System.out.println("Created a server socket with port number "
					+ portNum);
			this.routerDesc = rd;
			this.m_lsd = lsd;
			this.m_lsa = lsa;
		} catch (IOException e) {
			System.out.println("Cannot create server socket;");
		}

	}
	
	private boolean canAcceptIncomingConnection() {
		for(int i =0; i< m_ports.length; i++){
			if(m_ports[i] != null){
				continue;
			}else{
				return true;
			}
		}
		return false;
	}

	

	public void run() {
		Thread serverResponseThread = null;
		while (true) {
			if (sServer != null) {
				try {
					Socket newSocket;
					this.flag = canAcceptIncomingConnection();
					//Socket newSocket = sServer.accept();
					// && socketAddr.size() < 4
					if (this.flag) {
						// Accept incoming connections.
						newSocket = sServer.accept();
						
						synchronized (this) {
							if (!socketAddr.contains(newSocket.getRemoteSocketAddress())) {
								socketAddr.add(newSocket.getRemoteSocketAddress());
							}
						}
						//this.portNum = this.portNum + 1;
						System.out.println("The client with Ip Address "
								+ newSocket.getRemoteSocketAddress()
								+ " just connected to you.");
						// invoke an objectInput thread

						serverResponse = new ServerInputOutput(
								newSocket, routerDesc, m_ports, m_potentialNeighbors, socketAddr, m_lsd, m_lsa);
						this.flag = serverResponse.canAcceptIncomingConnection();
						if (this.flag) {
							serverResponseThread = new Thread(serverResponse);
							serverResponseThread.start();
						}
						else {
							
							System.out.println("Ports are full");
							sServer.close();
							break;
						}
					}
					else {
						
						System.out.println("The server ports are full/last connection. No more connection Allowed.");
						//serverResponseThread.destroy();
						sServer.close();
						break;
					}

				} catch (IOException ioe) {

					System.out.println("Could not create socket on port " + sServer.getLocalSocketAddress());
					
				}

			} else {
				System.out.println("ServerSocket does not exist");

			}

			System.out.print(">> ");
		}
	}
}

class ServerInputOutput implements Runnable {

	Socket server;
	SOSPFPacket packetFromClient;
	LinkStateDatabase mm_database;
	ObjectInputStream inStream;
	ObjectOutputStream outStream;
	ObjectInputStream confirm;
	RouterDescription serverRouter;
	Link[] mm_ports;
	Link[] mm_potentialNeighbors;
	ArrayList<SocketAddress> mm_socketAddr;
	boolean flag;
	LSA mm_lsa;
	
	public ServerInputOutput(Socket server, RouterDescription serverRouter,
			Link[] ports, Link[] m_potentialNeighbors, ArrayList<SocketAddress> socketAddr, LinkStateDatabase m_lsd, LSA m_lsa) {
		this.server = server;
		this.serverRouter = serverRouter;
		this.mm_ports = ports;
		this.mm_potentialNeighbors = m_potentialNeighbors;
		flag = canAcceptIncomingConnection();
		this.mm_socketAddr = socketAddr;
		this.mm_database = m_lsd;
		this.mm_lsa = m_lsa; 
		
		

	}
	public boolean canAcceptIncomingConnection() {
		for(int i =0; i< mm_ports.length; i++){
			if(mm_ports[i] != null){
				continue;
			}else{
				//System.out.println("valid index " +i);
				return true;
			}
		}
		return false;
	}

	public void run() {
		int nextAvailPort = 0;
		int nextAvailNeighbor = 0;
		
		while (true) {
			
			// server receives the message
			try {
				
				if (flag) {
					//System.out.println("Next avail =" + nextAvail);
					inStream = new ObjectInputStream(server.getInputStream());
					packetFromClient = (SOSPFPacket) inStream.readObject();
					
					System.out.println("PACKET SENT FROM " +packetFromClient.neighborID);
					
					System.out.println("PACKET TYPE: " +packetFromClient.sospfType);
					
					
						// Message received
						if (packetFromClient.sospfType == 0) {

							System.out.println("received HELLO from "
									+ packetFromClient.neighborID + "; ");
							

							nextAvailPort = isRouterPortAlreadyTaken(
									packetFromClient.neighborID,
									serverRouter.simulatedIPAddress,
									this.mm_ports);
							nextAvailNeighbor = isRouterPortAlreadyTaken(
									packetFromClient.neighborID,
									serverRouter.simulatedIPAddress,
									this.mm_potentialNeighbors);

							// System.out.println("isrouterporttaken " +
							// nextAvailPort);
							if (nextAvailPort >= 0) {

								// add to the server link
								// Router 2 is the client/sender IP --> look
								// into
								// the packet
								RouterDescription r2 = new RouterDescription(
										packetFromClient.srcProcessIP,
										packetFromClient.srcProcessPort,
										packetFromClient.neighborID,
										RouterStatus.INIT);
								// router1 = is the server IP
								RouterDescription r1 = new RouterDescription(
										serverRouter.processIPAddress,
										serverRouter.processPortNumber,
										serverRouter.simulatedIPAddress);
								Link l = new Link(r1, r2,
										packetFromClient.weight);
								mm_ports[nextAvailPort] = l;

								LinkDescription newLink = new LinkDescription();
								newLink.linkID = packetFromClient.neighborID;
								newLink.portNum = packetFromClient.srcProcessPort;
								newLink.tosMetrics = packetFromClient.weight;
								mm_lsa.links.add(newLink);
								mm_lsa.lsaSeqNumber++;

								// if the user did not do attach in this host
								if (nextAvailNeighbor >= 0) {

									mm_potentialNeighbors[nextAvailNeighbor] = l;
								}
								// if the user did attach previously, we should
								// override that because that was not determined
								else {
									mm_potentialNeighbors[nextAvailPort] = l;
								}

								System.out.println("set "
										+ r2.simulatedIPAddress + " state to "
										+ r2.status);

								// send back to the client
								outStream = new ObjectOutputStream(
										server.getOutputStream());
								// create a server packet and send it back to
								// the
								// client

								SOSPFPacket serverPacket = new SOSPFPacket(
										serverRouter.processIPAddress,
										serverRouter.processPortNumber,
										serverRouter.simulatedIPAddress,
										packetFromClient.neighborID, (short) 0,
										serverRouter.simulatedIPAddress,
										serverRouter.simulatedIPAddress,
										packetFromClient.weight);

								outStream.writeObject(serverPacket);

							} else {
								// second hello -- set to TWO_WAY
								int routerPosition = -1;
								for (int i = 0; i < mm_ports.length; i++) {
									// scan through the links
									if (mm_ports[i].router2.simulatedIPAddress
											.equals(packetFromClient.neighborID)) {

										mm_ports[i].router2.status = RouterStatus.TWO_WAY;
										System.out
												.println("set "
														+ mm_ports[i].router2.simulatedIPAddress
														+ " state to "
														+ mm_ports[i].router2.status);

										routerPosition = i;
										System.out.println("router position: "
												+ routerPosition);
										break;
									}
								}

								// prepare a packet with LSA of this current
								// router and send it back to client
								SOSPFPacket serverPacketForUpdate = generateFullPackage(
										(short) 1, packetFromClient);
								
								
								
								outStream = new ObjectOutputStream(
										server.getOutputStream());

								outStream.writeObject(serverPacketForUpdate);

							}
						}// done sending HELLO

						// broadcast received and the new packet received for
						// update
						else if (packetFromClient.sospfType == 1) {
							
							
								String incomingRouterIP = packetFromClient.neighborID;
								packetFromClient.originalSender = packetFromClient.neighborID;
								
								// update the database
								databaseUpdate(packetFromClient);
								
								// forward the incoming packet to the neighbors
								// except to the one that sent it
								packetFromClient.lsaArray.add(mm_lsa);
								
								// broadcast the update package
								broadcastToNeighbors(incomingRouterIP,
									packetFromClient);
						}
						else if (packetFromClient.sospfType == 2){
								
							
							boolean updated = databaseUpdate(packetFromClient);
							
							String originalIncoming = packetFromClient.neighborID;
							if (updated) {
								
								packetFromClient.neighborID = serverRouter.simulatedIPAddress;
								packetFromClient.lsaArray.add(mm_lsa);
								broadcastToNeighbors(originalIncoming,packetFromClient);
								
							}
							else {
								//check if the original sender is one of the neighbor, 
								//if it is, broadcast again
								packetFromClient.neighborID = serverRouter.simulatedIPAddress;
								packetFromClient.lsaArray.add(mm_lsa);
								packetFromClient.sospfType = 3;
								checkNeighbor(packetFromClient.originalSender, packetFromClient);
								
								
								
							}
							
							
								
						}
				

					
					
			 
				}//flag ends
				else {
					//Exception e = new Exception("Cannot receive message from client because ports are full");
					System.out.println("Cannot receive message from client because ports are full");
					break;
				}

			} catch (IOException e) {
				System.out.println("Cannot receive input object. Quit");
				e.printStackTrace();
				
				break;
				//System.out.println(server.getRemoteSocketAddress());
				/*
				//find the link in the potential Neighbor and Neighbor, then delete it in both arrays
				for (int i = 0; i < mm_socketAddr.size(); i++) {
					
					if (mm_socketAddr.get(i) != null) {
						//System.out.println(mm_socketAddr.get(i));
						if (mm_socketAddr.get(i).equals(server.getRemoteSocketAddress())) {
							
							
							System.out.println(i);
							
							mm_potentialNeighbors[i] = null;
							List<Link> list = new ArrayList<Link>(Arrays.asList(mm_potentialNeighbors));	
							
							list = new ArrayList<Link>(Arrays.asList(mm_potentialNeighbors));
							list.remove(Arrays.asList(mm_potentialNeighbors[i]));
							mm_potentialNeighbors = list.toArray(mm_potentialNeighbors);
							
							mm_ports[i] = null;
							List<Link> list1 = new ArrayList<Link>(Arrays.asList(mm_ports));
							list1 = new ArrayList<Link>(Arrays.asList(mm_ports));
							list1.remove(Arrays.asList(mm_ports[i]));
							mm_ports = list1.toArray(mm_ports);
								
							
							mm_socketAddr.remove(i);
						}	
					}
				}
				*/
				
				
				
			} catch (ClassNotFoundException ce) {

				System.out.println("Packet cannot be found");

			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}

	}
	
	private boolean checkNeighbor(String originalSender, SOSPFPacket updatePackage) {
		// TODO Auto-generated method stub
		boolean isNeighbor = false;
		Socket client;
		ObjectOutputStream outSend;
		
		for (int i = 0; i< this.mm_ports.length; i++) {
			
			if (mm_ports[i] != null){
				
				String neighbor = mm_ports[i].router2.simulatedIPAddress;
				if (neighbor.equals(originalSender)) {
					//send the packet back to the original sender
					try {
						client = new Socket(mm_ports[i].router2.processIPAddress, mm_ports[i].router2.processPortNumber);
						outSend = new ObjectOutputStream(
								client.getOutputStream());
						outSend.writeObject(updatePackage);
						
						
						//client.close();
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
					isNeighbor = true;
					break;
				}
			}
		}
		
		return isNeighbor;
	}
	
	private boolean databaseUpdate(SOSPFPacket incomingPacket) {
		
		boolean flag1 = false;
		boolean flag2 = false;
		
		
		
		
		System.out.println("UPDATING THE DATABASE!!!!!");
		for (int i = 0; i< incomingPacket.lsaArray.size(); i++) {
			if (incomingPacket.lsaArray.get(i) != null) {
				
				String sendIP = incomingPacket.lsaArray.get(i).linkStateID;
				int versionLSA = incomingPacket.lsaArray.get(i).lsaSeqNumber;
				
				if (!mm_database._store.containsKey(sendIP)) {
					mm_database.updateLSA(sendIP, incomingPacket.lsaArray.get(i));
					flag1 = true;
					
				}
				else if (mm_database._store.containsKey(sendIP) && versionLSA > mm_database._store.get(sendIP).lsaSeqNumber) {
					mm_database.updateLSA(sendIP, incomingPacket.lsaArray.get(i));
					flag2 = true;
				}
			}
			
		}
		
		
		System.out.println(mm_database.toString());
		
		return (flag1 || flag2);
		
	}
	
	
	
	private SOSPFPacket generateFullPackage(short type, SOSPFPacket incomingPacket) {
		
		System.out.println("PREPARING THE PACKET AND SEND BACK!!!\n\n");
		
		//create the package that only contains the LSA of this router
		SOSPFPacket serverPacketForUpdate = new SOSPFPacket(
				serverRouter.processIPAddress,
				serverRouter.processPortNumber,
				serverRouter.simulatedIPAddress,
				incomingPacket.neighborID, type,
				serverRouter.simulatedIPAddress,
				serverRouter.simulatedIPAddress, incomingPacket.weight);
		
		//Vector<LSA> clone = (Vector<LSA>)incomingPacket.lsaArray.clone();
		serverPacketForUpdate.lsaArray = incomingPacket.lsaArray;
		
		System.out.println("\n\n\nCURRENT VECTOR HOLDS!!!!!!!!!");
		System.out.println(serverPacketForUpdate.lsaArray.toString());
		
		
		serverPacketForUpdate.lsaArray.add(mm_lsa);
		
		System.out.println(mm_lsa.toString());
		
		return serverPacketForUpdate;
	}
	
	private void broadcastToNeighbors(String senderIP, SOSPFPacket updatePackage) {
		//the server has to send the update to all its neighbors execpt for the one that sent the LSA
		
		System.out.println("Broadcasting to neighbors");
		
		Socket[] clients = new Socket[4];
		ObjectOutputStream outBroadcast;
		updatePackage.sospfType = 2;
		
		
		for (int i = 0; i<mm_ports.length; i++) {
			
			if (mm_ports[i] != null) {
				if (!mm_ports[i].router2.simulatedIPAddress.equals(senderIP)) {
					try {
						clients[i] = new Socket(
								mm_ports[i].router2.processIPAddress,
								mm_ports[i].router2.processPortNumber);
						outBroadcast = new ObjectOutputStream(
								clients[i].getOutputStream());
						outBroadcast.writeObject(updatePackage);
						
						//outBroadcast.close();
						//clients[i].close();
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
		}
		
		
		
	}
	

	private int isRouterPortAlreadyTaken(String simIPAddr, String anotherIPAddr, Link[] mm_p) {
		for (int i = 0; i < mm_p.length; i++) {
			if (mm_p[i] != null) {
				if ( (mm_p[i].router2.simulatedIPAddress.equals(simIPAddr) && 
						mm_p[i].router1.simulatedIPAddress.equals(anotherIPAddr)) ||
						(mm_p[i].router2.simulatedIPAddress.equals(simIPAddr) && 
								mm_p[i].router1.simulatedIPAddress.equals(anotherIPAddr))) {
					return -1;
				} else {
					continue;
				}
			} else {
				return i;
			}
		}
		return -1;
	}

}

