package socs.network.node;

import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.Object;

public class Router {

	protected LinkStateDatabase lsd;
	RouterDescription rd = new RouterDescription();
	// assuming that all routers are with 4 ports
	Integer validPortNum = 0;
	Link[] ports = new Link[4];
	Link[] potentialNeighbors = new Link[4];
	
	Socket[] clients = new Socket[4];
	ServerServiceThread serThread;

	public Router(Configuration config, short portNum) {
		rd.simulatedIPAddress = config.getString("socs.network.router.ip");
		rd.processPortNumber = portNum;
		rd.processIPAddress = "127.0.0.1";
		initServerSocket(rd.processPortNumber); // initialize the server socket
		lsd = new LinkStateDatabase(rd);
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

	}

	/**
	 * disconnect with the router identified by the given destination ip address
	 * Notice: this command should trigger the synchronization of database
	 * 
	 * @param portNumber
	 *            the port number which the link attaches at
	 */
	private void processDisconnect(short portNumber) {

	}

	/**
	 * initialize the the server side socket
	 * 
	 */
	private void initServerSocket(short processPort) {
		serThread = new ServerServiceThread(processPort, rd, ports, potentialNeighbors);
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
					Link l = new Link(r1, r2);
					//ports[isAvail] = l;
					
					
				
					//System.out.println("Potential links updated in the client");
					//System.out.println("Link client: "+r2.simulatedIPAddress );
					potentialNeighbors[isAvail] = l;
					
					
					

				} else {
<<<<<<< HEAD
					
=======
					clients[isAvail].close();
>>>>>>> efd502fe633bdc31fd3fc6a8c853a21584ed6e25
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
	 * broadcast Hello to neighbors
	 * 
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	private void processStart() {
		
		int potential = 0;
		int portNum;
		int real = 0; 
		//check how many real links already established
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
		//System.out.println("ENTER HERE 1 !!!!");
		
		/**compare the real links with the potential links if the potential links is updated, then broadcast again, else stay idle**/
		if (potential > real) {
			ArrayList<Link> deleteLinks = new ArrayList<Link>();
			//System.out.println("ENTER HERE 2");
		// create 4 packets for each neighbor
		for (int i = 0; i < potentialNeighbors.length; i++) {
			
				if (this.potentialNeighbors[i] != null) {

<<<<<<< HEAD
					if (this.potentialNeighbors[i].router2.status == null) {
						try {
							if (clients[i] == null) {
								clients[i] = new Socket(
										potentialNeighbors[i].router2.processIPAddress,
										potentialNeighbors[i].router2.processPortNumber);
								//clients[i].setSoTimeout(1000);
=======
			if (this.ports[i] != null && ports[i].router2.status != RouterStatus.TWO_WAY) {
>>>>>>> efd502fe633bdc31fd3fc6a8c853a21584ed6e25

							}
							/** step 1 **/
							//System.out.println("START TO SEND PACKET");
							// client sends the packet to the server
							ObjectOutputStream outStreamToServer = new ObjectOutputStream(
									clients[i].getOutputStream());

							SOSPFPacket clientPacket = new SOSPFPacket(
									rd.processIPAddress,
									rd.processPortNumber,
									rd.simulatedIPAddress,
									potentialNeighbors[i].router2.simulatedIPAddress,
									(short) 0, rd.simulatedIPAddress,
									rd.simulatedIPAddress);

							outStreamToServer.writeObject(clientPacket);

						}
						catch (SocketTimeoutException st) {
							//if its socket timeout, delete the link from potential neighbors
							
							//potentialNeighbors[i] = null;
							//for (int j = i; i < potentialNeighbors.length-1; j++) {
							//	potentialNeighbors[j] = potentialNeighbors[j+1];
							//}
							//potentialNeighbors[potentialNeighbors.length-1] = null;
							
							//potentialNeighbors[i] = null;
							//List<Link> list = new ArrayList<Link>(Arrays.asList(potentialNeighbors));
							//list.removeAll(Arrays.asList(potentialNeighbors[i]));
							//potentialNeighbors = list.toArray(potentialNeighbors);
							deleteLinks.add(potentialNeighbors[i]);
							
						}
						catch (IOException e) {
							// TODO Auto-generated catch block
							System.out
									.println("Client cannot send to the object to the server " + potentialNeighbors[i].router2.simulatedIPAddress);
							//potentialNeighbors[i] = null;
							//List<Link> list = new ArrayList<Link>(Arrays.asList(potentialNeighbors));
							//list.removeAll(Arrays.asList(potentialNeighbors[i]));
							//potentialNeighbors = list.toArray(potentialNeighbors);
							
							
							
							deleteLinks.add(potentialNeighbors[i]);
							
							
							
							
						}
					}
				} else {
					break;
				}

		}// end the for loop
		
		
		
		// now try to receive the packets
<<<<<<< HEAD
		for (int i = 0; i < potentialNeighbors.length; i++) {
			if (potentialNeighbors[i] != null) {
				if (potentialNeighbors[i].router2.status == null) {
						try {
							/** The process of step 2 (client side) **/
							// System.out.println("Client tried to receive stuff");
							// client try receives the packet from the server
							ObjectInputStream inStreamFromServer = new ObjectInputStream(
									clients[i].getInputStream());

							SOSPFPacket packetFromServer = (SOSPFPacket) inStreamFromServer
									.readObject();

							// packet received
							if (packetFromServer.sospfType == 0) {
								// Client prints the HELLO message from server
								System.out.println("received HELLO from "
										+ packetFromServer.neighborID + "; ");

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
=======
		for (int i = 0; i < ports.length; i++) {
			if (ports[i] != null && ports[i].router2.status != RouterStatus.TWO_WAY) {
				try {
					/** The process of step 2 (client side) **/
					//System.out.println("Client tried to receive stuff");
					// client try receives the packet from the server
					ObjectInputStream inStreamFromServer = new ObjectInputStream(
							clients[i].getInputStream());
				
					SOSPFPacket packetFromServer = (SOSPFPacket) inStreamFromServer
							.readObject();

					// packet received
					if (packetFromServer.sospfType == 0) {
						// Client prints the HELLO message from server
						System.out.println("received HELLO from "
								+ packetFromServer.neighborID + "; ");

						// set the link to TWO_WAY

						for (int j = 0; j < 4; j++) {
							// find the Link that matched the packet information

							if (this.ports[j] != null) {
								if (ports[j].router2.simulatedIPAddress
										.equals(packetFromServer.neighborID)) {
									ports[j].router2.status = RouterStatus.TWO_WAY;
									System.out
											.println("set "
													+ ports[j].router2.simulatedIPAddress
													+ " state to "
													+ ports[j].router2.status);
									break;
>>>>>>> efd502fe633bdc31fd3fc6a8c853a21584ed6e25
								}
								/** The process of step 3 (client confirmation) **/

								SOSPFPacket responsePacket = new SOSPFPacket(
										rd.processIPAddress,
										rd.processPortNumber,
										rd.simulatedIPAddress,
										potentialNeighbors[i].router2.simulatedIPAddress,
										(short) 0, rd.simulatedIPAddress,
										rd.simulatedIPAddress);
								ObjectOutputStream confirmPacket = new ObjectOutputStream(
										clients[i].getOutputStream());
								confirmPacket.writeObject(responsePacket);

								// the potential neighbors link becomes real
								// neighbors
								// RouterDescription r2 = new
								// RouterDescription(potentialNeighbors.get(i).processIPAddress,
								// potentialNeighbors.get(i).processPortNumber,
								// potentialNeighbors.get(i).simulatedIPAddress);

								// RouterDescription r1 = new RouterDescription(
								// rd.processIPAddress, rd.processPortNumber,
								// rd.simulatedIPAddress);
								// Link l = new Link(r1, r2);
								ports[i] = potentialNeighbors[i];
								

							} else {
								System.out
										.println("Client did not receive a return message from the server");

							}
						
						}
						catch (SocketTimeoutException st) {
							//if its socket timeout, delete the link from potential neighbors
							System.out.println("Server ports are full, cannot send packets");
							
							//delete the link from potential neighbors
							List<Link> list = new ArrayList<Link>(Arrays.asList(potentialNeighbors));
							list.remove(Arrays.asList(potentialNeighbors[i]));
							potentialNeighbors = list.toArray(potentialNeighbors);
							
							
						}
						catch (IOException e) {
							// TODO Auto-generated catch block
							
							List<Link> list = new ArrayList<Link>(Arrays.asList(potentialNeighbors));
							list.remove(Arrays.asList(potentialNeighbors[i]));
							potentialNeighbors = list.toArray(potentialNeighbors);
							System.out.println("FAIL to receive connection from the server");
						} catch (ClassNotFoundException e) {
							System.out
									.println("Client cannot receive the packet from server");
						}
						
					}
			}
			
		}//end the for loop
		
		//remove the invalid neighbors if there are any
				if (deleteLinks.size() > 0) {
					
					for (int i = 0; i < deleteLinks.size(); i++) {
							
						List<Link> list = new ArrayList<Link>(Arrays.asList(potentialNeighbors));
						list.removeAll(Arrays.asList(deleteLinks.get(i)));
						potentialNeighbors = list.toArray(potentialNeighbors);
					}
						
				}
		
		
		}//end the check here 
		else {
			System.out.println("No more potential links. Real = Potential");
			
		}
		
	}//end the start method 

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

	}

	/**
	 * output the neighbors of the routers
	 */
	private void processNeighbors() {
		// find all the links of the node and print the IP address of the links
		for (int i = 0; i < ports.length; i++) {
<<<<<<< HEAD
			if (ports[i] != null && ports[i].router2.status.equals(RouterStatus.TWO_WAY)) {
				System.out.println("IP Address of the neighbor " + (i + 1)
						+ ": " + ports[i].router2.simulatedIPAddress);
=======
			if (ports[i] != null) {
				if(ports[i].router2.status == RouterStatus.TWO_WAY) {
					System.out.println("IP Address of the neighbor " + (i + 1)
							+ ": " + ports[i].router2.simulatedIPAddress);
				}
>>>>>>> efd502fe633bdc31fd3fc6a8c853a21584ed6e25
			}
		}
	}

	/**
	 * disconnect with all neighbors and quit the program
	 */
	private void processQuit() {

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
				} else if (command.equals("connect ")) {
					String[] cmdLine = command.split(" ");
					processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
							cmdLine[3], Short.parseShort(cmdLine[4]));
				} else if (command.equals("neighbors")) {
					// output neighbors
					processNeighbors();
				} else {
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

}

class ServerServiceThread implements Runnable {
	ServerSocket sServer;
	Link[] m_ports;
	Link[] m_potentialNeighbors;
	ArrayList<SocketAddress> socketAddr = new ArrayList<SocketAddress>();
	SOSPFPacket packet;
	RouterDescription routerDesc;
	ServerInputOutput serverResponse;
	boolean flag = true;
	int portNum = 0;
	

	public ServerServiceThread(short portNum, RouterDescription rd,
			Link[] ports, Link[] potentialNeighbors) {
			
		try {
			this.m_ports = ports;
			this.m_potentialNeighbors = potentialNeighbors;
			sServer = new ServerSocket(portNum);
			System.out.println("Created a server socket with port number "
					+ portNum);
			this.routerDesc = rd;
		} catch (IOException e) {
			System.out.println("Cannot create server socket;");
		}

	}
<<<<<<< HEAD
	
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

	
=======
>>>>>>> efd502fe633bdc31fd3fc6a8c853a21584ed6e25

	public void run() {
		Thread serverResponseThread = null;
		while (true) {
			if (sServer != null) {
				try {
					Socket newSocket;
					this.flag = canAcceptIncomingConnection();
					//Socket newSocket = sServer.accept();
					if (this.flag && socketAddr.size() < 4) {
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
								newSocket, routerDesc, m_ports, m_potentialNeighbors, socketAddr);
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
	ObjectInputStream inStream;
	ObjectOutputStream outStream;
	ObjectInputStream confirm;
	RouterDescription serverRouter;
	Link[] mm_ports;
	Link[] mm_potentialNeighbors;
	ArrayList<SocketAddress> mm_socketAddr;
	boolean flag;
	public ServerInputOutput(Socket server, RouterDescription serverRouter,
			Link[] ports, Link[] m_potentialNeighbors, ArrayList<SocketAddress> socketAddr) {
		this.server = server;
		this.serverRouter = serverRouter;
		this.mm_ports = ports;
		this.mm_potentialNeighbors = m_potentialNeighbors;
		flag = canAcceptIncomingConnection();
		this.mm_socketAddr = socketAddr;

		// get the number from the client side

	}
	public boolean canAcceptIncomingConnection() {
		for(int i =0; i< mm_ports.length; i++){
			if(mm_ports[i] != null){
				continue;
			}else{
<<<<<<< HEAD
				//System.out.println("valid index " +i);
=======
>>>>>>> efd502fe633bdc31fd3fc6a8c853a21584ed6e25
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

					// Message received
					if (packetFromClient.sospfType == 0) {

						System.out.println("received HELLO from "
								+ packetFromClient.neighborID + "; ");
						// System.out.println("serverrouter ip: " +
						// serverRouter.simulatedIPAddress +
						// " packet neighbor id: " + packetFromClient.neighborID
						// +
						// " packet router id: " + packetFromClient.routerID);

						nextAvailPort = isRouterPortAlreadyTaken(
								packetFromClient.neighborID,
								serverRouter.simulatedIPAddress, this.mm_ports);
						nextAvailNeighbor = isRouterPortAlreadyTaken(
								packetFromClient.neighborID,
								serverRouter.simulatedIPAddress, this.mm_potentialNeighbors);
						
						//System.out.println("isrouterporttaken " + nextAvailPort);
						if (nextAvailPort >= 0) {
							// add to the server link
							// Router 2 is the client/sender IP --> look into
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
							Link l = new Link(r1, r2);
							mm_ports[nextAvailPort] = l;
							//if the user did not do attach in this host 
							if (nextAvailNeighbor >= 0) {
								
								mm_potentialNeighbors[nextAvailNeighbor] = l;
							}
							//if the user did attach previously, we should override that because that was not determined
							else {
								mm_potentialNeighbors[nextAvailPort] = l;
							}
							
							
							System.out.println("set " + r2.simulatedIPAddress
									+ " state to " + r2.status);

							// send back to the client
							outStream = new ObjectOutputStream(
									server.getOutputStream());
							// create a server packet and send it back to the
							// client

							SOSPFPacket serverPacket = new SOSPFPacket(
									serverRouter.processIPAddress,
									serverRouter.processPortNumber,
									serverRouter.simulatedIPAddress,
									packetFromClient.neighborID, (short) 0,
									serverRouter.simulatedIPAddress,
									serverRouter.simulatedIPAddress);
						
							
							outStream.writeObject(serverPacket);

						} else {
							// second hello

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
									break;
								}
							}
							

							// send back the package again

							ObjectOutputStream outAgain = new ObjectOutputStream(
									server.getOutputStream());
							SOSPFPacket anotherServerPacket = new SOSPFPacket(
									serverRouter.processIPAddress,
									serverRouter.processPortNumber,
									serverRouter.simulatedIPAddress,
									packetFromClient.neighborID, (short) 0,
									serverRouter.simulatedIPAddress,
									serverRouter.simulatedIPAddress);
							outAgain.writeObject(anotherServerPacket);
							
							
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
<<<<<<< HEAD
				
				//System.out.println(server.getRemoteSocketAddress());
				
				//find the link in the potential Neighbor and Neighbor, then delete it in both arrays
				for (int i = 0; i < mm_socketAddr.size(); i++) {
					
					if (mm_socketAddr.get(i) != null) {
						System.out.println(mm_socketAddr.get(i));
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
				
				
				
=======
>>>>>>> efd502fe633bdc31fd3fc6a8c853a21584ed6e25
				break;
			} catch (ClassNotFoundException ce) {

				System.out.println("Packet cannot be found");

			} catch (Exception e1) {
				e1.printStackTrace();
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
