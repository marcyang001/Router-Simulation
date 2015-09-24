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
	ClientServiceThread cliThread;

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
		ServerServiceThread serThread = new ServerServiceThread(processPort, rd, ports);
        Thread t = new Thread(serThread); 
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
			
	        cliThread = new ClientServiceThread(rd, processIP, processPort, simulatedIP);
	        Thread t = new Thread(cliThread);
			t.start();
			// router 1 = localhost
			// router 2 = router you are sending to
			RouterDescription r2 = new RouterDescription(processIP, processPort, simulatedIP);
			RouterDescription r1 = new RouterDescription(rd.processIPAddress, rd.processPortNumber, rd.simulatedIPAddress, rd.status);
			Link l = new Link(r1, r2);
			ports[validPortNum] = l;
			validPortNum++;
			
			//implicitly send the package to the server
			
			
		}else{
			System.out.println("The link is already established.");
		}
	}
	
	/**
	 * broadcast Hello to neighbors
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	private void processStart() throws IOException {
		cliThread.publishMessageToServer();
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
	Link m_ports[];
	int acceptedLinkNum = 0;
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
		if(sServer != null)
		{
			System.out.println("Initializing the server: ");
			try
			{ 
	            // Accept incoming connections. 
	            Socket newSocket = sServer.accept(); 
	            
	            System.out.println("The client with Ip Address " + newSocket.getRemoteSocketAddress() + 
	            		" just connected to you.");

	            //server receives the message here
	            ObjectInputStream inStreamFromClient = new ObjectInputStream(newSocket.getInputStream());
				SOSPFPacket packet = (SOSPFPacket) inStreamFromClient.readObject();
				
				if(packet.sospfType == 0) {
					// say Hello~
					System.out.println("received HELLO from " + packet.neighborID + ";");
					// router 1 = localhost
					RouterDescription r2 = new RouterDescription(packet.srcProcessIP, packet.srcProcessPort, packet.srcIP, RouterStatus.INIT);
					RouterDescription r1 = new RouterDescription(routerDesc.processIPAddress, routerDesc.processPortNumber, routerDesc.simulatedIPAddress);
					Link l = new Link(r1, r2);
					m_ports[acceptedLinkNum] = l;
					System.out.println("set " + r2.simulatedIPAddress + " state to " + r2.status );
				}  
				acceptedLinkNum++;
			} 
			catch(IOException ioe) 
		    { 
				ioe.printStackTrace();
				System.out.println("Could not create server socket on port "+ sServer.getLocalSocketAddress() +". Quitting.");   
		    }
			catch (ClassNotFoundException ce)
			{
				ce.printStackTrace();
				System.out.println("Could not find the class SOSPFPacket");
			}
			
		
			
			
		}else {
			System.out.println("ServerSocket does not exist.");
			
		}
		System.out.print(">> ");
	}
}


class ClientServiceThread implements Runnable {
	RouterDescription rdServer;
	String m_destSimulatedIP;
	Socket client;
	String m_connectingIP;
	short m_connectingPort;
	public ClientServiceThread() {
		super();
	}

	ClientServiceThread(RouterDescription rdServer, String connectingIP, short connectingPort, String connectingSimulatedIP) {
		this.rdServer = rdServer;
		this.m_destSimulatedIP = connectingSimulatedIP;
		this.m_connectingIP = connectingIP;
		this.m_connectingPort = connectingPort;
	}
	
	public void run() {
		try {
			client = new Socket(m_connectingIP, m_connectingPort);
			System.out.println("Just connected to " + m_destSimulatedIP);
	
			System.out.print(">> ");
			
			
			//client.close();	
		} catch (IOException e) {
			System.out.print("Cannot connect to " + rdServer.processIPAddress + ", exception occured is " + e);
		}
	}
	// step 1
	public void publishMessageToServer() throws IOException {
		
		SOSPFPacket packet = new SOSPFPacket(rdServer.processIPAddress, rdServer.processPortNumber, 
				rdServer.simulatedIPAddress, this.m_destSimulatedIP, (short)0, rdServer.simulatedIPAddress, rdServer.simulatedIPAddress ); 
		
		ObjectOutputStream outStreamToServer = new ObjectOutputStream(this.client.getOutputStream());

		outStreamToServer.writeObject(packet);
		outStreamToServer.close();

	}
	

}

