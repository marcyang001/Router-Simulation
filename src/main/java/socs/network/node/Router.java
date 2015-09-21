package socs.network.node;

import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Router {

	protected LinkStateDatabase lsd;
	RouterDescription rd = new RouterDescription();
	int validPortNum = 0;
	// assuming that all routers are with 4 ports
	Link[] ports = new Link[4];

	public Router(Configuration config, short portNum) {
		rd.simulatedIPAddress = config.getString("socs.network.router.ip");
		rd.processPortNumber = portNum;
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
	
	private void initServerSocket(short processPort) {
		ServerServiceThread serThread = new ServerServiceThread(processPort);
        Thread t = new Thread(serThread); 
        t.start();
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
		
        ClientServiceThread cliThread = new ClientServiceThread(processIP,processPort);
        Thread t = new Thread(cliThread);
		t.start();

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

					if (validPortNum >= 4) {
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
			System.out.println("cannot create server socket;");
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
	            System.out.println("Initialized the server thread. Waiting for client communication.");
			} 
			catch(IOException ioe) 
		    { 
				System.out.println("Could not create server socket on port "+ sServer.getLocalSocketAddress() +". Quitting.");   
		    } 
		}else {
			System.out.println("serverSocket does not exist.");
		}
	}
}


class ClientServiceThread implements Runnable {
	String m_processIP;
	short m_serverPortNum;
	Link[] m_cliLink;
	public ClientServiceThread() {
		super();
	}

	ClientServiceThread(String processIP, short serverPortNum, Link[] link) {
		m_serverPortNum = serverPortNum;
		m_processIP = processIP;
		m_cliLink = link;
	}
	
	public void run() {
		try {
			Socket client = new Socket(m_processIP, m_serverPortNum);
			System.out.print("Just connected to " +  m_processIP);
			client.close();
			//establish the link for the first time
			/*
			 * Define: 
			 * router 1 is the localhost 
			 * router 2 is the router you are connecting to / receiving from
			 */
			
			if (m_cliLink.length == 0) {
				
				m_cliLink[0].router1.simulatedIPAddress = m_processIP;
				m_cliLink[0].router1.processPortNumber =
				
				
			}
		} catch (IOException e) {
			System.out.print("Cannot connect to " + m_processIP + ", exception occured is " + e);
		}
	}

}
