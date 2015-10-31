package socs.network.message;

import java.io.*;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class SOSPFPacket implements Serializable {

  //for inter-process communication
  public String srcProcessIP;
  public short srcProcessPort;

  //simulated IP address
  public String srcIP;
  public String dstIP;

  //common header
  public short sospfType; //0 - HELLO, 1 - LinkState Update
  // routerID = IP address of the router which sent this packet
  public String routerID;

  //used by HELLO message to identify the sender of the message
  //e.g. when router A sends HELLO to its neighbor, it has to fill this field with its own
  //simulated IP address
  // e.g. in HELLO message, it equals to src IP
  //in LSAUpdate packet, it will change for every hop
  // neighborID is also srcIP
  public String neighborID; //(sender's)neighbor's simulated IP address

  //used by LSAUPDATE
  public Vector<LSA> lsaArray = new Vector();
  public short weight;
  
  //use the package to do message treatment
  public SOSPFPacket(String srcProcessIP, short srcProcessPort, String srcIP, String destIP, short sospfType, 
		  String routerID, String neighborID, short weight) {
	  this.srcProcessIP = srcProcessIP;
	  this.srcProcessPort = srcProcessPort;
	  this.srcIP = srcIP;
	  this.dstIP = destIP;
	  this.sospfType = sospfType;
	  this.routerID = routerID;
	  this.neighborID = neighborID;
	  this.weight = weight;
  }
  
  
  
  

}
