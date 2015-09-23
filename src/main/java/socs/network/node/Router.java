package socs.network.node;

import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Router {

	protected LinkStateDatabase lsd;
	RouterDescription rd = new RouterDescription();
	int validPortNum;
	// assuming that all routers are with 4 ports
	Link[] ports = new Link[4];

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
		ServerServiceThread serThread = new ServerServiceThread(processPort);
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
	        ClientServiceThread cliThread = new ClientServiceThread(processIP,processPort);
	        Thread t = new Thread(cliThread);
			t.start();
			rd.status = RouterStatus.INIT;
			// router 1 = localhost
			// router 2 = router you are sending to
			RouterDescription r2 = new RouterDescription(processIP, processPort, simulatedIP, RouterStatus.INIT);
			RouterDescription r1 = new RouterDescription(rd.processIPAddress, rd.processPortNumber, rd.simulatedIPAddress, rd.status);
			Link l = new Link(r1, r2);
			ports[validPortNum] = l;
			validPortNum++;
		}else{
			System.out.println("The link is already established.");
		}
	}
	
	/**
	 * broadcast Hello to neighbors
	 */
	private void processStart() {

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
	public ServerServiceThread() {
		super();
	}

	public ServerServiceThread(short portNum) {
		try {
			sServer = new ServerSocket(portNum);
			System.out.println("Created a server socket with port number " + portNum );
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
	            System.out.print(">> ");
			} 
			catch(IOException ioe) 
		    { 
				System.out.println("Could not create server socket on port "+ sServer.getLocalSocketAddress() +". Quitting.");   
		    } 
		}else {
			System.out.println("ServerSocket does not exist.");
		}
	}
}


class ClientServiceThread implements Runnable {
	String m_processIP;
	short m_serverPortNum;
	public ClientServiceThread() {
		super();
	}

	ClientServiceThread(String processIP, short serverPortNum) {
		m_serverPortNum = serverPortNum;
		m_processIP = processIP;
		
	}
	
	public void run() {
		try {
			Socket client = new Socket(m_processIP, m_serverPortNum);
			System.out.println("Just connected to " +  m_processIP);
			System.out.print(">> ");
			
			client.close();	
		} catch (IOException e) {
			System.out.print("Cannot connect to " + m_processIP + ", exception occured is " + e);
		}
	}

}

class ClientBroadcastThread implements Runnable {
	
	/**
	 * 
	 * this thread serves for broadcasting all neighbors
	 */
	Socket m_client;
	String m_simulatedIP;
	
	//pass in a socket client. 
	//just type in ClientBroadcastThread cliBroadcast = new ClientBroadcastThread(rd.SimulatedIP, new Socket(serverName, port))
	public ClientBroadcastThread(String localSimulatedIP, Socket client) {
		this.m_client = client;
		this.m_simulatedIP = localSimulatedIP;
	}
	
	
	
	public void run() {
		// TODO Auto-generated method stub
		
		OutputStream outToServer;
		try {
			outToServer = m_client.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);
	        out.writeUTF("Hello from "
	                     + m_simulatedIP);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Cannot emit messages to neighbors");
		}
        
		
		
	}
	
	
	
}

