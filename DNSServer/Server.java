package DNSServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Location;

public class Server {
	private String serverName;
	private String serverIP;
	private double[] serverLocation = new double[2];

	public Server(String name, String ip) {
		serverName = name;
		serverIP = ip;
		CityResponse response;
		try {
			if (DNSServer.databaseReader != null) {
				response = DNSServer.databaseReader.city(InetAddress
						.getByName(serverIP));
				Location locationServer = response.getLocation();
				serverLocation[0] = locationServer.getLatitude();
				serverLocation[1] = locationServer.getLongitude();
			}
		} catch (UnknownHostException e) {
			return;
		} catch (IOException e) {
			return;
		} catch (GeoIp2Exception e) {
			return;
		}
	}

	public String getName() {
		// TODO Auto-generated method stub
		return serverName;
	}

	public String getIP() {
		return serverIP;
	}

	public double[] getLocation() {
		return serverLocation;
	}
}
