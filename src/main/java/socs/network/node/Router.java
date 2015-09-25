package socs.network.node;

import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Router {

	protected LinkStateDatabase lsd;
	RouterDescription rd = new RouterDescription();
	int validPortNum;
	// assuming that all routers are with 4 ports
	Link[] ports = new Link[4];
	Socket[] clients =new Socket[4];
	ServerServiceThread serThread;

	public Router(Configuration config, short portNum) {
		validPortNum = 0;
		rd.simulatedIPAddress = config.getString("socs.network.router.ip");
		rd.processPortNumber = portNum;
		rd.processIPAddress = "127.0.0.1";
		initServerSocket(rd.processPortNumber); //initialize the server socket
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
        t.setDaemon(true);
        t.start();
        
        
        
	}

	/**
	 * 
	 * helper function to check if the router would be mapped twice
	 * @return -1 if already taken, or the array index of the next available slot 
	 * 
	 */
	private boolean isRouterPortAlreadyTaken( String simIPAddr ) {
		for(int i=0; i<ports.length; i++) {
			if(ports[i] != null) {
				if(ports[i].router2.simulatedIPAddress.equals(simIPAddr)) {
					return false;
				}else {
					continue;
				}
			}else {
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
			String simulatedIP, short weight) throws Exception {
		
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
		boolean isAvail = isRouterPortAlreadyTaken(simulatedIP);
		if(isAvail) {
			
	        //cliThread = new ClientServiceThread(rd, processIP, processPort, simulatedIP);
	        //Thread t = new Thread(cliThread);
			//t.start();
			// router 1 = localhost
			// router 2 = router you are sending to
			clients[validPortNum] = new Socket(processIP, processPort);
			System.out.println("Just connected to " + clients[validPortNum].getRemoteSocketAddress());
	
			//System.out.print(">> ");
			
			RouterDescription r2 = new RouterDescription(processIP, processPort, simulatedIP);
			RouterDescription r1 = new RouterDescription(rd.processIPAddress, rd.processPortNumber, rd.simulatedIPAddress);
			Link l = new Link(r1, r2);
			ports[validPortNum] = l;
			validPortNum++;
			
			//implicitly send the package to the server
			
			
		}else{
			System.out.println("The link is already established from client.");
		}
	}
	
	/**
	 * broadcast Hello to neighbors
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	private void processStart() {
		
		
		//create 4 packets for each neighbor
		for (int i = 0; i < 4; i++) {
			if (this.ports[i] != null) {
				
				System.out.println("There exists a valid port " + i);
				try {
					/** step 1 **/ 
					//client sends the packet to the server
					ObjectOutputStream outStreamToServer = new ObjectOutputStream(clients[i].getOutputStream());
					SOSPFPacket clientPacket = new SOSPFPacket(rd.processIPAddress, rd.processPortNumber, rd.simulatedIPAddress, ports[i].router2.simulatedIPAddress, (short) 0, 
							rd.simulatedIPAddress, rd.simulatedIPAddress);
					System.out.println("Object : " + clientPacket.srcIP);
					outStreamToServer.writeObject(clientPacket);
					
					
					/**The process of step 2 (client side)**/
					//client try receives the packet from the server
					ObjectInputStream inStreamFromServer = new ObjectInputStream(clients[i].getInputStream());
					SOSPFPacket packetFromServer = (SOSPFPacket) inStreamFromServer.readObject();
					
					//packet received
					if (packetFromServer.sospfType == 0) {
						//Client prints the HELLO message from server
						System.out.println("received HELLO from " + packetFromServer.neighborID + "; ");
						
						//set the link to TWO_WAY 
						ports[validPortNum -1].router2.status = RouterStatus.TWO_WAY;
						
						System.out.println("set " + ports[validPortNum -1].router2.simulatedIPAddress +" state to " + ports[validPortNum -1].router2.status);
						
					}
					else {
						System.out.println("Client did not receive a return message from the server");

					}
					
					/**The process of step 3 (client confirmation)**/
					ObjectOutputStream confirmPacket = new ObjectOutputStream(clients[i].getOutputStream());
					confirmPacket.writeObject(clientPacket);
					
					
					
					
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("Client cannot send to the object to the server ");
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					System.out.println("Client cannot receive the packet from server");
				}

				
				
			}
			else {
				break;
			}
			
		}//end the for loop
		
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
		for(int i = 0; i<ports.length; i++) {
			if( ports[i] != null ) {
				System.out.println("IP Address of the neighbor " + i+1 + ": " + 
					ports[i].router2.simulatedIPAddress);
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

					if (validPortNum >= 4 ) {
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
	Socket newSocket;
	Link m_ports[];
	SOSPFPacket packet;
	int firstTimeReceiving = 0;
	RouterDescription routerDesc;
	
	
	public ServerServiceThread() {
		super();
	}
	public ServerServiceThread(short portNum, RouterDescription rd, Link ports[]) {
		try {
			this.m_ports = ports;
			sServer = new ServerSocket(portNum);
			System.out.println("Created a server socket with port number " + portNum );
			this.routerDesc = rd;
		} catch (IOException e) {
			System.out.println("Cannot create server socket;");
		}
			
	}
	
	public void run() {
		while (true) {
			if (sServer != null) {

				// System.out.println("Initializing the server: ");
				try {
					// Accept incoming connections.
					newSocket = sServer.accept();

					System.out.println("The client with Ip Address "
							+ newSocket.getRemoteSocketAddress()
							+ " just connected to you.");
					
					//add links here 
					
					
					
					//invoke an objectInput thread
					
					ServerInputOutput serverResponse = new ServerInputOutput(newSocket, routerDesc, m_ports);
					Thread serverResponseThread = new Thread(serverResponse);
					
					serverResponseThread.start();
					
					


				} catch (IOException ioe) {
					
					System.out.println("Could not create server socket on port "
									+ sServer.getLocalSocketAddress()
									+ ". Quitting.");
				}// catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				//}

			} else {
				System.out.println("ServerSocket does not exist.");

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
	int acceptedLinks = 0;
	
	public ServerInputOutput(Socket server, RouterDescription serverRouter, Link[] ports) {
		this.server = server;
		this.serverRouter = serverRouter;
		this.mm_ports = ports;
	}
	
	public void run() {
		while (true) {
			
			
			//server receives the message
			try {
				inStream = new ObjectInputStream(server.getInputStream());
				packetFromClient = (SOSPFPacket) inStream.readObject();
				
				//Message received 
				if(packetFromClient.sospfType == 0 ) {
					
					System.out.println("received HELLO from " + packetFromClient.neighborID + "; ");
					
					
					boolean isAvail = isRouterPortAlreadyTaken(packetFromClient.neighborID);
					if(isAvail) {
						//add to the server link
						//Router 2 is the client/sender IP --> look into the packet
						RouterDescription r2 = new RouterDescription(packetFromClient.srcProcessIP, packetFromClient.srcProcessPort, packetFromClient.neighborID, RouterStatus.INIT);
						//router1 = is the server IP 
						RouterDescription r1 = new RouterDescription(serverRouter.processIPAddress, serverRouter.processPortNumber, serverRouter.simulatedIPAddress);
						Link l = new Link(r1, r2);
						mm_ports[acceptedLinks] = l;
						acceptedLinks++;
						
						System.out.println("set " + r2.simulatedIPAddress + "state to " + r2.status);
						
						//send back to the client
						outStream = new ObjectOutputStream(server.getOutputStream());
						//create a server packet and send it back to the client
						
						
						SOSPFPacket serverPacket = new SOSPFPacket(serverRouter.processIPAddress, serverRouter.processPortNumber, serverRouter.simulatedIPAddress, packetFromClient.neighborID,
								(short)0, serverRouter.simulatedIPAddress, serverRouter.simulatedIPAddress);						
						//System.out.println("server packet: " + serverPacket.neighborID);
						outStream.writeObject(serverPacket);
						
						
						
					}else{
						
						//when the server receives the package again from the same client (twice)
						//System.out.println("The link is already established.");
						
						if (packetFromClient.sospfType == 0) {
							int index = acceptedLinks - 1;
							mm_ports[index].router2.status = RouterStatus.TWO_WAY;					
							System.out.println("set " + mm_ports[index].router2.simulatedIPAddress + "state to " + mm_ports[index].router2.status);
							
							//send back the package again
							
							ObjectOutputStream outAgain = new ObjectOutputStream(server.getOutputStream());
							SOSPFPacket anotherServerPacket = new SOSPFPacket(serverRouter.processIPAddress, serverRouter.processPortNumber, serverRouter.simulatedIPAddress, packetFromClient.neighborID,
									(short)0, serverRouter.simulatedIPAddress, serverRouter.simulatedIPAddress);
							outAgain.writeObject(anotherServerPacket);
							
						}
						
					}
					
					
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Cannot receive input object");
			}
			catch (ClassNotFoundException ce) {
				
				System.out.println("Packet cannot be found");
				
			}
			
			
		}
		
	}
	private boolean isRouterPortAlreadyTaken( String simIPAddr ) {
		for(int i=0; i<mm_ports.length; i++) {
			if(mm_ports[i] != null) {
				if(mm_ports[i].router2.simulatedIPAddress.equals(simIPAddr)) {
					return false;
				}else {
					continue;
				}
			}else {
				return true;
			}
		}
		return false;
	}
	
}



