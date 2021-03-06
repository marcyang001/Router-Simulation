package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Router {

	protected LinkStateDatabase lsd;
	protected LSA lsa;
	
	RouterDescription rd = new RouterDescription();
	// assuming that all routers are with 4 ports
	Integer validPortNum = 0;
	Link[] ports = new Link[4];
	Link[] potentialNeighbors = new Link[4];
	SignalMessage[] sendMessage = new SignalMessage[4];
	Socket[] clients = new Socket[4];
	ServerServiceThread serThread;
	Thread[] t = new Thread[4]; //set the timers for each neighbor
	
	public Router(Configuration config, short portNum) {
		rd.simulatedIPAddress = config.getString("socs.network.router.ip");
		rd.processPortNumber = portNum;
		rd.processIPAddress = "127.0.0.1";
		lsd = new LinkStateDatabase(rd);
		lsa = new LSA();
		lsa.linkStateID = rd.simulatedIPAddress;
		initServerSocket(rd.processPortNumber, lsd, lsa); // initialize the server socket
	}

	/**
	 * output the shortest path to the given destination ip
	 * <p/>
	 * format: source ip address -> ip address -> ... -> destination ip
	 * 
	 * @param destinationIP
	 *            the ip adderss of the destination simulated router
	 */
	private void processDetect(String destinationIP) {

		String s = lsd.getShortestPath(destinationIP);
		System.out.println(s);

	}
	
	private int isNeighbor(short portNumber) {
		int isNeighbor = -1;
		for (int i = 0; i< lsa.links.size(); i++) {
			if (lsa.links.get(i).portNum == portNumber) {
				isNeighbor = i;
			}
		}
		
		return isNeighbor;
	}
	
	
	
	/**
	 * disconnect with the router identified by the given destination ip address
	 * Notice: this command should trigger the synchronization of database
	 * 
	 * @param portNumber
	 *            the port number which the link attaches at
	 */
	private void processDisconnect(short portNumber) {
		Socket client = null;
		
		
		//find if the port exists as a neighbor
		int isNeighbor = isNeighbor(portNumber);
		if (isNeighbor >=0) {
			
			
			//store the disconnected neighbor IP
			//LinkDescription disconnectedLink = lsa.links.get(isNeighbor);
			String disconnectNode = lsa.links.get(isNeighbor).linkID;
			System.out.println("PORT NUMBER " +portNumber + " IS A NEIGHBOR: " + disconnectNode);
			
			try {
				client = new Socket("localhost", portNumber);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//remove the disconnected neighbor from LINKS
			lsa.links.remove(isNeighbor);
			lsa.lsaSeqNumber++;
			lsd.updateLSA(rd.simulatedIPAddress, lsa);
			deleteNeighborLink(portNumber);
			//do the reverse
			lsd.deleteLinkFromANeighbor(disconnectNode, rd.simulatedIPAddress);
			
			
			
			if (ports[0] == null && potentialNeighbors[0]== null) {
				lsd.cleanAll();
			}
			else {
				lsd.clean();
			}

			
			
			SOSPFPacket responsePacket = new SOSPFPacket(
					rd.processIPAddress,
					rd.processPortNumber,
					rd.simulatedIPAddress,
					disconnectNode,
					(short) 6, rd.simulatedIPAddress,
					rd.simulatedIPAddress, (short)-1);
			
			responsePacket.lsaArray = lsd.retrieveLSAs();
			responsePacket.lsaArray.add(lsa);
			responsePacket.originalSender = rd.simulatedIPAddress;
			
			
			//SOSPFPacket newPacket = generateFullPacketUpdate((short)6, responsePacket);
			
			ObjectOutputStream disconnectSignal = null;
			try {
				disconnectSignal = new ObjectOutputStream(client.getOutputStream());
				disconnectSignal.writeObject(responsePacket);
			}
			catch (IOException e) {
				System.out.println("FAIL TO SEND A DISCONNECT MESSAGE");
			}
			
			
			
			responsePacket.originalSender = disconnectNode;
			broadcastToNeighbors(disconnectNode, responsePacket, (short)7);
			
			//System.out.println("THEN: " + m_router.lsa.lsaSeqNumber);
			System.out.println(lsd.toString());
			
			
		}
		else {
			System.out.println("PORT NUMBER: " + portNumber+ " IS NOT A CONNECTED NEIGHBOR");
		}
	}

	/**
	 * initialize the the server side socket
	 * @param processPort
	 * @param lsd
	 * @param lsa
	 * 
	 */
	private void initServerSocket(short processPort, LinkStateDatabase lsd, LSA lsa) {
		serThread = new ServerServiceThread(processPort, rd, ports, potentialNeighbors, lsd, lsa);
		Thread t = new Thread(serThread);
		t.start();

	}

	/**
	 * 
	 * helper function to check if the router would be mapped twice
	 * 
	 * @return -1 if already taken, or the array index of the next available
	 *         slot
	 * 
	 */
	private int isRouterPortAlreadyTaken(String simIPAddr, String mySimIPAddr, Link[] p) {
		for (int i = 0; i < p.length; i++) {
			if (p[i] != null) {
				if ((p[i].router2.simulatedIPAddress.equals(simIPAddr) && 
						p[i].router1.simulatedIPAddress.equals(mySimIPAddr)) ||
						(p[i].router1.simulatedIPAddress.equals(simIPAddr) && 
								p[i].router2.simulatedIPAddress.equals(mySimIPAddr)))	{
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
	
	public boolean canAcceptIncomingConnection() {
		for(int i =0; i< ports.length; i++){
			if(ports[i] != null){
				continue;
			}else{
				return true;
			}
		}
		return false;
	}

	/**
	 * attach the link to the remote router, which is identified by the given
	 * simulated ip; to establish the connection via socket, you need to
	 * identify the process IP and process Port; additionally, weight is the
	 * cost to transmitting data through the link
	 * <p/>
	 * NOTE: this command should not trigger link database synchronization
	 */
	private void processAttach(String processIP, short processPort,
			String simulatedIP, short weight) {
		/**
		// accept() will block until a client connects to the server.
		// If execution reaches this point, then it means that a client
		// socket has been accepted.

		// For each client, we will start a service thread to
		// service the client requests. This is to demonstrate a
		// Multi-Threaded server. Starting a thread also lets our
		// MultiThreadedSocketServer accept multiple connections simultaneously.

		// Start a Client Service thread
		// router 1 = localhost
		// router 2 = router you are sending to
		**/
		int isAvail = isRouterPortAlreadyTaken(simulatedIP, rd.simulatedIPAddress, potentialNeighbors);
		validPortNum = isAvail;
		if (isAvail >= 0) {

			
			// router 1 = localhost
			// router 2 = router you are sending to
			try {
				clients[isAvail] = new Socket(processIP, processPort);
				clients[isAvail].setSoTimeout(2000);
				if (clients[isAvail].isConnected()) {
					System.out.println("Just connected to " + simulatedIP);
					
					//add to potential neighbors
					
					
					
					RouterDescription r2 = new RouterDescription(processIP,
							processPort, simulatedIP);
					
					RouterDescription r1 = new RouterDescription(
							rd.processIPAddress, rd.processPortNumber,
							rd.simulatedIPAddress);
					Link l = new Link(r1, r2, weight);

									
					potentialNeighbors[isAvail] = l;
					
					
					

				} else {
					
					clients[isAvail] = null;
					clients[isAvail].close();
					System.out.println("Could not make the connection. Check if the server's ports are full or server socket is open");
				}
			} catch (ConnectException e) {
				// TODO Auto-generated catch block
				System.out.println("Connection is refused by the server");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			


		} else {
			System.out.println("The link is already established or client ports are full.");
		}
	}

	/**
	 * broadcast the updated LSA to the neighbors except to the one that sends the LSA
	 *  **/
	protected void broadcastToNeighbors(String senderIP, SOSPFPacket updatePackage, short type) {		
		System.out.println("Broadcasting to neighbors from client");
		ObjectOutputStream outBroadcast;
		updatePackage.sospfType = type;
		Socket broadcastClients[] = new Socket[4];

		for (int i = 0; i< ports.length; i++) {
			
			if (ports[i] != null) {
				
				//System.out.println("BROADCASTING TO NEIGHBORS: " + ports[i].router2.simulatedIPAddress);
				if (!ports[i].router2.simulatedIPAddress.equals(senderIP)) {
					//broadcast the new lsa to the neighbors
					//System.out.println("BROADCASTING TO NEIGHBORS: " + ports[i].router2.simulatedIPAddress);
					try {
						broadcastClients[i] = new Socket(
								ports[i].router2.processIPAddress,
								ports[i].router2.processPortNumber);
						
						outBroadcast = new ObjectOutputStream(broadcastClients[i].getOutputStream());
						outBroadcast.writeObject(updatePackage);
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	
				}

			}			
		}
		
		
	}
	
	
	/**
	 * broadcast Hello to neighbors
	 * 
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	private void processStart() {

		int potential = 0;
		int portNum;
		int real = 0;
		ObjectOutputStream outStreamToServer;
		// check how many real links already established
		for (portNum = 0; portNum < 4; portNum++) {
			if (this.ports[portNum] != null) {
				real++;
			}
			if (this.potentialNeighbors[portNum] != null) {
				potential++;
			}
		}
		System.out.println("real neighbors: " + real);
		System.out.println("potential neighbors: " + potential);
		// System.out.println("ENTER HERE 1 !!!!");

		/**
		 * compare the real links with the potential links if the potential
		 * links is updated, then broadcast again, else stay idle
		 **/
		if (potential > real) {
			ArrayList<Link> deleteLinks = new ArrayList<Link>();
			// System.out.println("ENTER HERE 2");
			// create 4 packets for each neighbor
			for (int i = 0; i < potentialNeighbors.length; i++) {

				if (this.potentialNeighbors[i] != null) {

					if (this.potentialNeighbors[i].router2.status == null) {
						try {
							if (clients[i] == null) {
								clients[i] = new Socket(
										potentialNeighbors[i].router2.processIPAddress,
										potentialNeighbors[i].router2.processPortNumber);
								// clients[i].setSoTimeout(1000);

							}
							/** step 1 **/
							// System.out.println("START TO SEND PACKET");
							// client sends the packet to the server
							outStreamToServer = new ObjectOutputStream(
									clients[i].getOutputStream());

							SOSPFPacket clientPacket = new SOSPFPacket(
									rd.processIPAddress,
									rd.processPortNumber,
									rd.simulatedIPAddress,
									potentialNeighbors[i].router2.simulatedIPAddress,
									(short) 0, rd.simulatedIPAddress,
									rd.simulatedIPAddress,
									potentialNeighbors[i].weight);

							outStreamToServer.writeObject(clientPacket);

						} catch (SocketTimeoutException st) {
							// if its socket timeout, delete the link from
							// potential neighbors
							this.potentialNeighbors[i].router2.status = RouterStatus.FAIL;

							deleteLinks.add(potentialNeighbors[i]);

						} catch (IOException e) {
							// TODO Auto-generated catch block
							System.out
									.println("Client cannot send to the object to the server "
											+ potentialNeighbors[i].router2.simulatedIPAddress);
							this.potentialNeighbors[i].router2.status = RouterStatus.FAIL;
							
							deleteLinks.add(potentialNeighbors[i]);

						}
					}
				} else {
					break;
				}

			}// end the for loop

			// now try to receive the packets
			for (int i = 0; i < potentialNeighbors.length; i++) {
				if (potentialNeighbors[i] != null) {
					ObjectInputStream inStreamFromServer;
					if (potentialNeighbors[i].router2.status == null) {
						if (potentialNeighbors[i].router2.status != RouterStatus.FAIL) {

							try {
								/** The process of step 2 (client side) **/
								// System.out.println("Client tried to receive stuff");
								// client try receives the packet from the
								// server
								inStreamFromServer = new ObjectInputStream(
										clients[i].getInputStream());

								SOSPFPacket packetFromServer = (SOSPFPacket) inStreamFromServer
										.readObject();

								// packet received
								if (packetFromServer.sospfType == 0) {
									// Client prints the HELLO message from
									// server
									System.out.println("received HELLO from "
											+ packetFromServer.neighborID
											+ "; ");

									// set the link to TWO_WAY

									for (int j = 0; j < 4; j++) {
										// find the Link that matched the packet
										// information

										if (this.potentialNeighbors[j].router2 != null) {
											if (potentialNeighbors[j].router2.simulatedIPAddress
													.equals(packetFromServer.neighborID)) {
												potentialNeighbors[j].router2.status = RouterStatus.TWO_WAY;
												System.out
														.println("set "
																+ potentialNeighbors[j].router2.simulatedIPAddress
																+ " state to "
																+ potentialNeighbors[j].router2.status);
												break;
											}
										}
									}

									LinkDescription newNeighborLink = new LinkDescription();
									newNeighborLink.linkID = potentialNeighbors[i].router2.simulatedIPAddress;
									newNeighborLink.portNum = potentialNeighbors[i].router2.processPortNumber;
									newNeighborLink.tosMetrics = potentialNeighbors[i].weight;

									lsa.links.add(newNeighborLink);
									lsa.lsaSeqNumber++;

									/**
									 * The process of step 3 (client
									 * confirmation)
									 * 
									 * send the packet with LSA of the client
									 * **/

									SOSPFPacket responsePacket = new SOSPFPacket(
											rd.processIPAddress,
											rd.processPortNumber,
											rd.simulatedIPAddress,
											potentialNeighbors[i].router2.simulatedIPAddress,
											(short) 0, rd.simulatedIPAddress,
											rd.simulatedIPAddress,
											potentialNeighbors[i].weight);

									responsePacket.lsaArray.add(lsa);

									ObjectOutputStream confirmPacket = new ObjectOutputStream(
											clients[i].getOutputStream());
									confirmPacket.writeObject(responsePacket);

									// the potential neighbors link becomes real
									// neighbors

									int portAvail = isRouterPortAlreadyTaken(
											potentialNeighbors[i].router2.simulatedIPAddress,
											rd.simulatedIPAddress, ports);
									if (portAvail >= 0) {
										ports[portAvail] = potentialNeighbors[i];
									} else {
										deleteLinks.add(potentialNeighbors[i]);
									}

								} else {
									potentialNeighbors[i].router2.status = RouterStatus.FAIL;
									System.out
											.println("Client did not receive a return message from the server");
									deleteLinks.add(potentialNeighbors[i]);

								}
								
								/**
								 * now try to synchronize the database: 1.
								 * receive a packet with LSA of server 2. update
								 * to the database 3. broadcast to neighbors 4.
								 * send back a packet with LSA of client
								 * **/

								if (potentialNeighbors[i].router2.status == RouterStatus.TWO_WAY) {
									System.out.println("ENTER HERE FOR UPDATE");
									try {
										// if (ports[i] != null) {
										if (clients[i] != null) {

											inStreamFromServer = new ObjectInputStream(
													clients[i].getInputStream());

											SOSPFPacket packetFromServerForUpdate = (SOSPFPacket) inStreamFromServer
													.readObject();

											// now try to synchronize the
											// database:
											/**
											 * 1. receive a packet with LSA of
											 * server 2. update to the database
											 * 3. broadcast to neighbors 4. send
											 * back a packet with LSA of client
											 * **/

											// ready for link state update
											if (packetFromServerForUpdate.sospfType == 1) {
												
												// update the database
												databaseUpdate(packetFromServerForUpdate);

												// prepare its own package and
												// send back to the server

												SOSPFPacket backToServerPacket = generateFullPacketUpdate(
														(short) 1,
														packetFromServerForUpdate);

												outStreamToServer = new ObjectOutputStream(
														clients[i]
																.getOutputStream());
												outStreamToServer
														.writeObject(backToServerPacket);

												// forward the package to its
												// neighbors with its own LSA
												broadcastToNeighbors(
														packetFromServerForUpdate.neighborID,
														backToServerPacket, (short)2);

											}

											// spawn off the child thread that
											// sends the message periodically
											sendMessage[i] = new SignalMessage(ports[i], clients[i], this);
											
											t[i] = new Thread(sendMessage[i]);
											t[i].start();

										} else {
											System.out
													.println("Client is disconnected");
											break;
										}
										// }

									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (ClassNotFoundException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}

								} else {
									System.out
											.println("NOT TWO WAY: not connected to "
													+ potentialNeighbors[i].router2.simulatedIPAddress);
								}

							} catch (SocketTimeoutException st) {
								// if its socket timeout, delete the link from
								// potential neighbors
								System.out
										.println("Server ports are full, cannot send packets");

								// delete the link from potential neighbors
								List<Link> list = new ArrayList<Link>(
										Arrays.asList(potentialNeighbors));
								list.remove(Arrays
										.asList(potentialNeighbors[i]));
								potentialNeighbors = list
										.toArray(potentialNeighbors);

							} catch (IOException e) {
								// TODO Auto-generated catch block

								List<Link> list = new ArrayList<Link>(
										Arrays.asList(potentialNeighbors));
								list.remove(Arrays
										.asList(potentialNeighbors[i]));
								potentialNeighbors = list
										.toArray(potentialNeighbors);
								System.out
										.println("FAIL to receive connection from the server");

								e.printStackTrace();

							} catch (ClassNotFoundException e) {
								System.out
										.println("Client cannot receive the packet from server");
							}

						}// !FAIL
					}
				}

			}// end the for loop

			// remove the invalid neighbors if there are any
			if (deleteLinks.size() > 0) {

				for (int i = 0; i < deleteLinks.size(); i++) {

					List<Link> list = new ArrayList<Link>(
							Arrays.asList(potentialNeighbors));
					list.removeAll(Arrays.asList(deleteLinks.get(i)));
					potentialNeighbors = list.toArray(potentialNeighbors);
				}

			}
		}// end the check here
		else {
			System.out.println("No more potential links. Real = Potential");

		}

	}// end the start method
	
	
	private boolean databaseUpdate(SOSPFPacket incomingPacket) {
		
		boolean flag1 = false;
		boolean flag2 = false;
		
		//try to update the LSA to the database
		for (int j = 0; j < incomingPacket.lsaArray.size(); j++) {
			
			if (incomingPacket.lsaArray.get(j) != null) {
				
				String senderOfLSA = incomingPacket.lsaArray.get(j).linkStateID;
				int versionOfLSA = incomingPacket.lsaArray.get(j).lsaSeqNumber;
				//check if the LSA already existed in the database
				if (!lsd._store.containsKey(senderOfLSA)) {
					//update to the database
					lsd.updateLSA(senderOfLSA, incomingPacket.lsaArray.get(j));
					flag1 = true;
				}
				//LSA already existed, check if its the newest version
				else if (lsd._store.containsKey(senderOfLSA)) {
					if (lsd._store.get(senderOfLSA).lsaSeqNumber < versionOfLSA) {
						lsd.updateLSA(senderOfLSA, incomingPacket.lsaArray.get(j));
						flag2 = true;
					}
					
				}
			}
			
		}
		
		System.out.println("UPDATED THE DATABASE IN THE CLIENT");
		System.out.println(lsd.toString());
		
		return (flag1 || flag2);
		
	}
	public boolean deleteNeighborLink(short portNumber) {
		
		List<Link> list = new ArrayList<Link>(Arrays.asList(ports));
		List<Link> listPot = new ArrayList<Link>(Arrays.asList(potentialNeighbors));
		List<Thread> listTime = new ArrayList<Thread>(Arrays.asList(t));
		boolean status = false;
		for (int i = 0; i< ports.length; i++) {
			if (ports[i] != null) {
				if (ports[i].router2.processPortNumber == portNumber) {
					
					System.out.println("REMOVE THE NEIGHBOR LINK FROM PORT!!!!!!");
					//kill the timer
					t[i] = null;	
					listTime.removeAll(Arrays.asList(t[i]));
					t = listTime.toArray(t);
					//remove the port
					list.removeAll(Arrays.asList(ports[i]));
					ports = list.toArray(ports);
					for (int j = 0; j < potentialNeighbors.length; j++) {
						if (potentialNeighbors[j] != null) {
							if (potentialNeighbors[j].router2.processPortNumber == portNumber) {
								System.out.println("REMOVE THE LINK FROM POTENTIAL NEIGHBORS!!!!!!");
								listPot.removeAll(Arrays.asList(potentialNeighbors[j]));
								potentialNeighbors = listPot.toArray(potentialNeighbors);
								break;
							}
						}	
					}
					status = true;
					
					break;
				}
			}
		}
		return status;
	}
	
	
	
	
	
	public SOSPFPacket generateFullPacketUpdate(short type, SOSPFPacket incomingPacket) {
		//create the package that only contains the LSA of this router
		SOSPFPacket serverPacketForUpdate = new SOSPFPacket(
				rd.processIPAddress, rd.processPortNumber,
				rd.simulatedIPAddress, incomingPacket.neighborID, type,
				rd.simulatedIPAddress, rd.simulatedIPAddress,
				incomingPacket.weight);
		serverPacketForUpdate.originalSender = incomingPacket.originalSender;
		serverPacketForUpdate.lsaArray = lsd.retrieveLSAs();
		serverPacketForUpdate.lsaArray.addAll(incomingPacket.lsaArray);
		serverPacketForUpdate.lsaArray.add(lsa);
		
		
		return serverPacketForUpdate;
				
				
	}
	/**
	 * 
	 * @param l
	 * @param posInPort: index position in port array 
	 * @param posInPot: index position in potentialNeighbor array 
	 * 
	 * returns the LinkDescription object of new neighbor
	 * @return 
	 */
	
	private LinkDescription addNeighborLink(Link l, int posInPort, int posInPot) {
		
		this.potentialNeighbors[posInPot] = l;
		this.ports[posInPort] = l;
		
		LinkDescription newNeighbor = new LinkDescription();
		newNeighbor.linkID = l.router2.simulatedIPAddress;
		newNeighbor.portNum = l.router2.processPortNumber;
		newNeighbor.tosMetrics = l.weight;
		
		return newNeighbor;
	}
	
	
	
	
	
	/**
	 * attach the link to the remote router, which is identified by the given
	 * simulated ip; to establish the connection via socket, you need to
	 * identify the process IP and process Port; additionally, weight is the
	 * cost to transmitting data through the link
	 * <p/>
	 * This command does trigger the link database synchronization
	 */
	private void processConnect(String processIP, short processPort,
			String simulatedIP, short weight) {
		Socket connect = null;
		ObjectOutputStream outStream = null;
		ObjectInputStream inStream = null;
		
		int freeIndex = isRouterPortAlreadyTaken(simulatedIP, rd.simulatedIPAddress, ports);
		int freePos = isRouterPortAlreadyTaken(simulatedIP, rd.simulatedIPAddress, potentialNeighbors);
		if (freeIndex >=0 && freePos >= 0) {
			
			RouterDescription r2 = new RouterDescription(processIP, processPort, simulatedIP);
			
			//build the link 
			Link l = new Link(rd, r2, weight);
			
			
			
			
			try {
				connect = new Socket(processIP, processPort);
				
				SOSPFPacket clientPacket = new SOSPFPacket(
						rd.processIPAddress,
						rd.processPortNumber,
						rd.simulatedIPAddress,
						r2.simulatedIPAddress,
						(short) 0, rd.simulatedIPAddress,
						rd.simulatedIPAddress,
						weight);
				
				SOSPFPacket connectPackage = this.generateFullPacketUpdate((short)0, clientPacket);
				//send the first packet for HELLO message
				outStream = new ObjectOutputStream(connect.getOutputStream());
				outStream.writeObject(connectPackage);
				//receive back the LSA from the server
				inStream = new ObjectInputStream(connect.getInputStream());
				SOSPFPacket respPacket = (SOSPFPacket) inStream.readObject();
				
				if (respPacket.sospfType == 0) {
					
					LinkDescription nl = addNeighborLink(l, freeIndex, freePos);
					
					
					lsa.links.add(nl);
					lsa.lsaSeqNumber++;
					
					potentialNeighbors[freePos].router2.status = RouterStatus.TWO_WAY;
					ports[freeIndex].router2.status = RouterStatus.TWO_WAY;
					//second hello 
					outStream = new ObjectOutputStream(connect.getOutputStream());
					outStream.writeObject(connectPackage);
				
					//receive this after second hello
					inStream = new ObjectInputStream(connect.getInputStream());
					respPacket = (SOSPFPacket) inStream.readObject();
					if (respPacket.sospfType == 1) {
						
						databaseUpdate(respPacket);
						
						// prepare its own package and
						// send back to the server

						SOSPFPacket backToServerPacket = generateFullPacketUpdate(
								(short) 1,
								respPacket);
						outStream = new ObjectOutputStream(connect.getOutputStream());
						outStream.writeObject(backToServerPacket);

						// forward the package to its
						// neighbors with its own LSA
						broadcastToNeighbors(respPacket.neighborID,
								backToServerPacket, (short)2);
						
						sendMessage[freeIndex] = new SignalMessage(ports[freeIndex], connect, this);
						
						t[freeIndex] = new Thread(sendMessage[freeIndex]);
						t[freeIndex].start();
					}
				}
				else {
					
					System.out
							.println("Client did not receive a return message from the server");

				}
				
				
				
				
				
				
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("FAIL TO CONNECT TO THE SERVER");
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
			
			
		}
		else {
			System.out.println("PORTS FOR THIS ROUTER ARE FULL / LINK ALREADY ESTABLISHED");
		}
	}

	

	/**
	 * output the neighbors of the routers
	 */
	private void processNeighbors() {
		// find all the links of the node and print the IP address of the links
		for (int i = 0; i < ports.length; i++) {
			if (ports[i] != null && !ports[i].router2.status.equals(RouterStatus.INIT)) {
				System.out.println("IP Address of the neighbor " + (i + 1)
						+ ": " + ports[i].router2.simulatedIPAddress);
			}
		}
	}

	/**
	 * disconnect with all neighbors and quit the program
	 */
	private void processQuit() {
		
		System.exit(1);

	}

	public void terminal() {
		try {
			InputStreamReader isReader = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader(isReader);
			System.out.print(">> ");
			String command = br.readLine();
			while (true) {
				if (command.startsWith("detect ")) {
					String[] cmdLine = command.split(" ");
					processDetect(cmdLine[1]);
				} else if (command.startsWith("disconnect ")) {
					String[] cmdLine = command.split(" ");
					processDisconnect(Short.parseShort(cmdLine[1]));
				} else if (command.startsWith("quit")) {
					processQuit();
				} else if (command.startsWith("attach ")) {
	
					String[] cmdLine = command.split(" ");
					processAttach(cmdLine[1], Short.parseShort(cmdLine[2]), 
							cmdLine[3], Short.parseShort(cmdLine[4]));
				} else if (command.equals("start")) {
					processStart();
				} else if (command.startsWith("connect ")) {
					String[] cmdLine = command.split(" ");
					processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
							cmdLine[3], Short.parseShort(cmdLine[4]));
				} else if (command.equals("neighbors")) {
					// output neighbors
					processNeighbors();
				} else if (command.equals("check")) {
					processCheck();
					
				}else if (command.equals("source")) {
					processSource();
				}
				else {
					// invalid command
					System.out.println("Invalid command. Quitting");
					break;
				}
				System.out.print(">> ");
				command = br.readLine();
			}
			isReader.close();
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**helper function for testing, not necessary **/
	private void processSource() {
		// TODO Auto-generated method stub
		System.out.println("Current source is: " + rd.simulatedIPAddress);
		
	}

	/**helper function for testing, not necessary **/
	private void processCheck() {
		// TODO Auto-generated method stub
		System.out.println("CURRENT LSA OF THE ROUTER: ");
		System.out.println(lsa.toString());
		System.out.println("CURRENT DATABASE OF THE ROUTER:");
		System.out.println(lsd.toString());
		
	}

}
