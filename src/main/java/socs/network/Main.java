package socs.network;

import socs.network.node.Router;
import socs.network.util.Configuration;

public class Main {

	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("usage: program conf_path");
			System.exit(1);
		}
		short portNum = (short)Integer.parseInt(args[1]);
		 
		Router r = new Router(new Configuration(args[0]), portNum);
		r.terminal();
	}

}
