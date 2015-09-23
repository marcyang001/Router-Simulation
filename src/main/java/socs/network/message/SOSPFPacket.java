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
  public String routerID;

  //used by HELLO message to identify the sender of the message
  //e.g. when router A sends HELLO to its neighbor, it has to fill this field with its own
  //simulated IP address
  public String neighborID; //(sender's)neighbor's simulated IP address

  //used by LSAUPDATE
  public Vector<LSA> lsaArray = null;
  
  //use the package to do message treatment
  public SOSPFPacket(String srcProcessIP, short srcProcessPort, String srcIP, String destIP, short sospfType, String routerID, String neighborID) {
	  this.srcProcessIP = srcProcessIP;
	  this.srcProcessPort = srcProcessPort;
	  this.srcIP = srcIP;
	  this.dstIP = destIP;
	  this.sospfType = sospfType;
	  this.routerID = routerID;
	  this.neighborID = neighborID;
  }
  
  
  
  

}
