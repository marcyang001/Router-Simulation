package socs.network.node;

public class RouterDescription {
  //used to socket communication
  String processIPAddress;
  short processPortNumber;
  //used to identify the router in the simulated network space
  String simulatedIPAddress;
  //status of the router
  RouterStatus status;
  
  public RouterDescription() {};

  public RouterDescription(String ipAddr, short portNum, String simIpAddr, RouterStatus status) {
	  this.processIPAddress = ipAddr;
	  this.processPortNumber = portNum;
	  this.simulatedIPAddress = simIpAddr;
	  this.status = status;
  }
}
