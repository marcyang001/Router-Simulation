package socs.network.node;

import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Router {

	protected LinkStateDatabase lsd;
	RouterDescription rd = new RouterDescription();
	
	
	private ServerSocket serverSocket = null;
	private Socket clientSocket = null;

	int validPortNum = 0;
	// assuming that all routers are with 4 ports
	Link[] ports = new Link[4];

	public Router(Configuration config, short processPort) {
		rd.simulatedIPAddress = config.getString("socs.network.router.ip");
		rd.processIPAddress = "127.0.0.1";
		rd.processPortNumber = processPort;
		rd.status = RouterStatus.INIT;
		lsd = new LinkStateDatabase(rd);
		
		initServerSocket();
		
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
	
	private boolean initServerSocket() {
	
		boolean serverOn = true;
		try
		{ 
			serverSocket = new ServerSocket(rd.processPortNumber); 
			System.out.println("Initialize the server on port: "+ rd.processPortNumber);
			return serverOn;
	    } 
		catch(IOException ioe) 
	    { 
			System.out.println("Could not create server socket on port "+ rd.processPortNumber +". Quitting.");
	            
	        serverOn = false;
	        return serverOn;
	    } 
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
		//here is the change 

		/*
		 * the client is trying to the server. 
		 * 1. init the multithreaded server
		 * 2. connect the client to the multithreaded server
		 */
		
		
		//establish the link between router 1 (host) and router 2 (client)
		//router 1 (host) 
		ports[validPortNum].router1.processIPAddress = rd.simulatedIPAddress;
		ports[validPortNum].router1.processPortNumber = processPort;
	
		//router 2 (client)
		ports[validPortNum].router2.processIPAddress = simulatedIP;
		ports[validPortNum].router2.processPortNumber = processPort;
		weight = (short) (weight +1);
		validPortNum++;
		
		
		
		
		
		
		
		
		
		
		//System.out.println("Current simulated IP is : " + simulatedIP);
		
		

	}
	

	
	

	/**
	 * broadcast Hello to neighbors
	 */
	private void processStart() {

	}

	/**
	 * attach the link to the remote router, which is identified by the given
	 * simulated ip; to establish the connection via socket, you need to
	 * indentify the process IP and process Port; additionally, weight is the
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




class ClientServiceThread implements Runnable {
	Socket myClientSocket;
	boolean m_bRunThread = true;

	public ClientServiceThread() {
		super();
	}

	ClientServiceThread(Socket s) {
		myClientSocket = s;

	}
	
	public void run() {
		// TODO Auto-generated method stub
		
	}
}
