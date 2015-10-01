package socs.network.node;

import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Router {

	protected LinkStateDatabase lsd;
	RouterDescription rd = new RouterDescription();
	// assuming that all routers are with 4 ports
	Integer validPortNum = 0;
	Link[] ports = new Link[4];
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
		serThread = new ServerServiceThread(processPort, rd, ports);
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
	private int isRouterPortAlreadyTaken(String simIPAddr, String mySimIPAddr) {
		for (int i = 0; i < ports.length; i++) {
			if (ports[i] != null) {
				if ((ports[i].router2.simulatedIPAddress.equals(simIPAddr) && 
						ports[i].router1.simulatedIPAddress.equals(mySimIPAddr)) ||
						(ports[i].router1.simulatedIPAddress.equals(simIPAddr) && 
								ports[i].router2.simulatedIPAddress.equals(mySimIPAddr)))	{
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
		int isAvail = isRouterPortAlreadyTaken(simulatedIP, rd.simulatedIPAddress);
		validPortNum = isAvail;
		if (isAvail >= 0) {

			// cliThread = new ClientServiceThread(rd, processIP, processPort,
			// simulatedIP);
			// Thread t = new Thread(cliThread);
			// t.start();
			// router 1 = localhost
			// router 2 = router you are sending to
			try {
				clients[isAvail] = new Socket(processIP, processPort);
				clients[isAvail].setSoTimeout(2000);
				if (clients[isAvail].isConnected()) {
					System.out.println("Just connected to " + simulatedIP);

					RouterDescription r2 = new RouterDescription(processIP,
							processPort, simulatedIP);
					RouterDescription r1 = new RouterDescription(
							rd.processIPAddress, rd.processPortNumber,
							rd.simulatedIPAddress);
					Link l = new Link(r1, r2);
					ports[isAvail] = l;
					

				} else {
					clients[isAvail] = null;
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

		// create 4 packets for each neighbor
		for (int i = 0; i < ports.length; i++) {

			if (this.ports[i] != null) {

//				System.out.println("There exists a valid port " + i);
//				System.out.println("router 1: " + ports[i].router1.simulatedIPAddress + " router 2: " +
//						ports[i].router2.simulatedIPAddress + " rd simIP: "+ rd.simulatedIPAddress);
				try {
					if (clients[i] == null) {
						clients[i] = new Socket(
								ports[i].router2.processIPAddress,
								ports[i].router2.processPortNumber);
						clients[i].setSoTimeout(1000);
					
					}
					/** step 1 **/
					// client sends the packet to the server
					ObjectOutputStream outStreamToServer = new ObjectOutputStream(
							clients[i].getOutputStream());
					
					SOSPFPacket clientPacket = new SOSPFPacket(
							rd.processIPAddress, rd.processPortNumber,
							rd.simulatedIPAddress,
							ports[i].router2.simulatedIPAddress, (short) 0,
							rd.simulatedIPAddress, rd.simulatedIPAddress);

					outStreamToServer.writeObject(clientPacket);

				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("Client cannot send to the object to the server ");
				}

			} else {
				break;
			}

		}// end the for loop
		// now try to receive the packets
		for (int i = 0; i < ports.length; i++) {
			if (ports[i] != null) {
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
								}
							}
						}

					} else {
						System.out.println("Client did not receive a return message from the server");

					}

					/** The process of step 3 (client confirmation) **/
	
					SOSPFPacket responsePacket = new SOSPFPacket(
							rd.processIPAddress, rd.processPortNumber,
							rd.simulatedIPAddress,
							ports[i].router2.simulatedIPAddress, (short) 0,
							rd.simulatedIPAddress, rd.simulatedIPAddress);
					ObjectOutputStream confirmPacket = new ObjectOutputStream(
							clients[i].getOutputStream());
					confirmPacket.writeObject(responsePacket);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					System.out.println("Client cannot receive the packet from server");
				}
			}
		}

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

	}

	/**
	 * output the neighbors of the routers
	 */
	private void processNeighbors() {
		// find all the links of the node and print the IP address of the links
		for (int i = 0; i < ports.length; i++) {
			if (ports[i] != null) {
				System.out.println("IP Address of the neighbor " + (i + 1)
						+ ": " + ports[i].router2.simulatedIPAddress);
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

					if (validPortNum < 0) {
						System.out.println("The router's ports are full");
					} else {
						String[] cmdLine = command.split(" ");
						processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
								cmdLine[3], Short.parseShort(cmdLine[4]));
					}

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
	SOSPFPacket packet;
	RouterDescription routerDesc;
	ServerInputOutput serverResponse;
	boolean flag = true;
	int portNum = 0;
	public ServerServiceThread() {
		super();
	}

	public ServerServiceThread(short portNum, RouterDescription rd,
			Link[] ports) {
		try {
			this.m_ports = ports;
			sServer = new ServerSocket(portNum);
			System.out.println("Created a server socket with port number "
					+ portNum);
			this.routerDesc = rd;
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
					//Socket newSocket = sServer.accept();
					if (this.flag && this.portNum < 4) {
						// Accept incoming connections.
						newSocket = sServer.accept();
						this.portNum = this.portNum + 1;
						System.out.println("The client with Ip Address "
								+ newSocket.getRemoteSocketAddress()
								+ " just connected to you.");
						// invoke an objectInput thread

						serverResponse = new ServerInputOutput(
								newSocket, routerDesc, m_ports);
						this.flag = serverResponse.canAcceptIncomingConnection();
						if (this.flag) {
							serverResponseThread = new Thread(serverResponse);
							serverResponseThread.start();
						}
						else {
							
							System.out.println("Ports are full");
							newSocket.close();
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
	boolean flag;
	public ServerInputOutput(Socket server, RouterDescription serverRouter,
			Link[] ports) {
		this.server = server;
		this.serverRouter = serverRouter;
		this.mm_ports = ports;
		flag = canAcceptIncomingConnection();
		// get the number from the client side

	}
	public boolean canAcceptIncomingConnection() {
		for(int i =0; i< mm_ports.length; i++){
			if(mm_ports[i] != null){
				continue;
			}else{
				System.out.println("valid index " +i);
				return true;
			}
		}
		return false;
	}

	public void run() {
		int nextAvail = 0;
		
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

						nextAvail = isRouterPortAlreadyTaken(
								packetFromClient.neighborID,
								serverRouter.simulatedIPAddress);
						// System.out.println("isrouterporttaken " + nextAvail);
						if (nextAvail >= 0) {
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
							mm_ports[nextAvail] = l;

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
							// System.out.println("server packet: " +
							// serverPacket.neighborID);
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
			} catch (ClassNotFoundException ce) {

				System.out.println("Packet cannot be found");

			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}

	}

	private int isRouterPortAlreadyTaken(String simIPAddr, String anotherIPAddr) {
		for (int i = 0; i < mm_ports.length; i++) {
			if (mm_ports[i] != null) {
				if ( (mm_ports[i].router2.simulatedIPAddress.equals(simIPAddr) && 
						mm_ports[i].router1.simulatedIPAddress.equals(anotherIPAddr)) ||
						(mm_ports[i].router2.simulatedIPAddress.equals(simIPAddr) && 
								mm_ports[i].router1.simulatedIPAddress.equals(anotherIPAddr))) {
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
