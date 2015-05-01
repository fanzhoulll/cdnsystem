package DNSServer;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Hashtable;
import java.util.Vector;

import com.maxmind.geoip2.DatabaseReader;

public class DNSServer {

	public static DatabaseReader databaseReader;
	public static int NUMBER_SERVER = 9;
	public static Vector<Server> serverList;
	public static int port; // This port must be the same with HTTP server port
	// DNS cache format:
	// Client IP: Server IP, Latency
	public static Hashtable<String, String[]> dnsCache;

	public DNSServer(int port, String acceptQuestion) {
		try {
			DNSServer.port = port;
			DatagramSocket socket = new DatagramSocket(port);
			System.out.println("Server Running at port: " + port);
			File database = new File(
					"/home/zhoufan1/rollCDN/GeoLite2-City.mmdb");
			if (database.exists()) {
				databaseReader = new DatabaseReader.Builder(database).build();
			} else {
				System.out
						.println("Cannot find geolocation database, still can run the CDN, but performance will be influenced");
			}
			initServer();
			dnsCache = new Hashtable<String, String[]>();
			while (true) {
				byte[] buf = new byte[256];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				new DNSServerThread(socket, packet, acceptQuestion).start();
			}
		} catch (IOException e) {
			System.out.println("Server error!");
		}
	}

	private void initServer() {
		serverList = new Vector<Server>(NUMBER_SERVER);
		Server server1 = new Server("Virginia", "52.0.73.113");
		Server server2 = new Server("Oregon", "52.11.8.29");
		Server server3 = new Server("California", "52.8.12.101");
		Server server4 = new Server("Ireland", "52.16.219.28");
		Server server5 = new Server("Frankfurt", "52.28.48.84");
		Server server6 = new Server("Tokyo", "52.68.12.77");
		Server server7 = new Server("Singapore", "52.74.143.5");
		Server server8 = new Server("Sydney", "52.64.63.125");
		Server server9 = new Server("SaoPaulo", "54.94.214.108");
		serverList.add(server1);
		serverList.add(server2);
		serverList.add(server3);
		serverList.add(server4);
		serverList.add(server5);
		serverList.add(server6);
		serverList.add(server7);
		serverList.add(server8);
		serverList.add(server9);
	}

	public static void main(String[] args) {
		if (!ifArgumentValid(args)) {
			System.out.println("Usage: ./dnsserver -p <port> -n <name>");
			System.out.println("Parameter: port: 40000-65535, name: host name");
			return;
		}
		int port = Integer.parseInt(args[1]);
		String acceptQuestion = args[3];
		new DNSServer(port, acceptQuestion);
	}

	private static boolean ifArgumentValid(String[] args) {
		if (args.length != 4)
			return false;
		if (!args[0].equals("-p"))
			return false;
		if (!args[1].matches("^-?\\d+$"))
			return false;
		if (!args[2].equals("-n"))
			return false;
		return true;
	}
}
