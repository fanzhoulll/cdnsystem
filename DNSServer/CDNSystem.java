package DNSServer;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.*;

public class CDNSystem {
	
	private static final double EARTH_RADIUS = 6378.137;
	private static final String RECORD_NOTYPE = "0";
	private static final String RECORD_CHANGING = "-1";
	public static final long TTL = 180 * 1000; // 3 minutes
	public static final double ERROR = 5.0;  // in ms
	
	

	public static String getBestServerIP(String newClientIP) {
		if (DNSServer.dnsCache.containsKey(newClientIP)) {
			String[] record = DNSServer.dnsCache.get(newClientIP);
			String serverIP = record[0];
			String recordTime = record[2];
			Long timeRecord = Long.parseLong(DNSServer.dnsCache.get(newClientIP)[2]);
			
			String timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar
					.getInstance().getTime());
			System.out.println("Time: " + timeStamp + " Request from: "
					+ newClientIP + " || Response IP: " + serverIP);
			
			if ((System.currentTimeMillis() - timeRecord > TTL && !recordTime.equals(RECORD_CHANGING)) || recordTime.equals(RECORD_NOTYPE))
				new updateBestServerThread(newClientIP);
			return serverIP;
		} else {
			String cloestServerIp;
			if (DNSServer.databaseReader != null)
				cloestServerIp =  getCloestServerIP(newClientIP);
			else
				cloestServerIp = getRandomServerIP();
			String newServerRecord[] = new String[3];
			newServerRecord[0] = cloestServerIp;
			newServerRecord[1] = RECORD_NOTYPE; // No delay information yet
			newServerRecord[2] = Long.toString(System.currentTimeMillis());
			DNSServer.dnsCache.put(newClientIP, newServerRecord);
			// After a new client first come, update best server for it so that next round this new client can 
			// use the best server
			
			String timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar
					.getInstance().getTime());
			System.out.println("Time: " + timeStamp + " Request from: "
					+ newClientIP + " || Response IP: " + cloestServerIp);
			
			new updateBestServerThread(newClientIP);
			return cloestServerIp;
		}
	}
	
	private static String getRandomServerIP() {
		int randomIndex = randInt(0, DNSServer.serverList.size() - 1);
		return DNSServer.serverList.elementAt(randomIndex).getIP();
	}
	
	public static int randInt(int min, int max) {

	    // NOTE: Usually this should be a field rather than a method
	    // variable so that it is not re-seeded every call.
	    Random rand = new Random();

	    // nextInt is normally exclusive of the top value,
	    // so add 1 to make it inclusive
	    int randomNum = rand.nextInt((max - min) + 1) + min;

	    return randomNum;
	}
	
	static class updateBestServerThread extends Thread
	{
		private String clientIP = ""; 
		
		public updateBestServerThread(String IP)
		{
			clientIP = IP;
			// Update record time, temporarily set it to be 0
			String[] temRecord = DNSServer.dnsCache.get(clientIP);
			temRecord[2] = RECORD_CHANGING;
			DNSServer.dnsCache.put(clientIP, temRecord);
			start();
		}
		
		public void run()
		{
			double[] delayData = new double[DNSServer.NUMBER_SERVER];
			for (int i = 0; i < DNSServer.NUMBER_SERVER; i++)
			{
				Server server = DNSServer.serverList.elementAt(i);
				// Important! This is the totalDelay, which is the sum of delay 
				// between HTTP Server -- clinet + HTTP Server + Original Server
				double delay = getDelayFromServer(clientIP, server.getIP());
				System.out.println("From " + server.getIP() + " To " + clientIP + ": " + delay);
				delayData[i] = delay;
			}
			int shortestDelayIndex = getShortestDelayIndex(delayData);
			double shortestDelay = delayData[shortestDelayIndex];
			if (!DNSServer.dnsCache.containsKey(clientIP))
				return;
			String[] oldServerRecord = DNSServer.dnsCache.get(clientIP);
			double oldDelay = Double.parseDouble(oldServerRecord[1]);
			// Two situation we should update record:
			// (1) New shortest delay < original delay - Error, means we find a better server
			// (2) original delay is zero, which means this is a new client, we just got its delay information
			if (shortestDelay <= oldDelay - ERROR || oldDelay == 0)
			{
				String[] newServerRecord = new String[3];
				newServerRecord[0] = DNSServer.serverList.elementAt(shortestDelayIndex).getIP();
				System.out.println(":) For client " + clientIP + " from " + oldServerRecord[0] + " to " + newServerRecord[0]);
				newServerRecord[1] = Double.toString(shortestDelay);
				newServerRecord[2] = Long.toString(System.currentTimeMillis());
				DNSServer.dnsCache.put(clientIP, newServerRecord);
			}
		}

		private int getShortestDelayIndex(double[] delayData) {
			double shortestDelay = Double.POSITIVE_INFINITY;
			int shortestDelayIndex = 0;
			for (int i = 0; i < delayData.length; i++) {
				double delay = delayData[i];
				if (delay < shortestDelay && delay != 0) {
					shortestDelay = delay;
					shortestDelayIndex = i;
				}
			}
			return shortestDelayIndex;
		}

		private double getDelayFromServer(String clientIP, String serverIP) {
			Socket client = new Socket();
			double delay = 0;
			SocketAddress sockAddr = new InetSocketAddress(serverIP, DNSServer.port);
			try {
				client.connect(sockAddr, 1000);
				Writer writer = new OutputStreamWriter(
						client.getOutputStream());
				writer.write("IP" + clientIP + "\n");
				writer.write("\n");
				writer.flush();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(client.getInputStream()));
				String message;
				while ((message = reader.readLine()) != null) {
					if (message.length() > 0)
					{
						try
						{

							delay = Double.parseDouble(message);
						}
						catch (Exception e)
						{
							System.out.println("Fail to get delay from server: " + serverIP);
							delay = 0;
						}
					}
				}
				reader.close();
				writer.close();
				client.close();
				return delay;
			} catch (IOException e) {
				System.out.println("Fail to get server " + serverIP);
				return delay;
			}
		}
	}
	
	private static String getCloestServerIP(String newClientIP) {
		try {
			InetAddress ipAddress = InetAddress.getByName(newClientIP);
			CityResponse response;
			try {
				response = DNSServer.databaseReader.city(ipAddress);
			} catch (GeoIp2Exception e) {
				return null;
			}
			Server cloestServer = chooseCloest(DNSServer.serverList, response.getLocation());
			return cloestServer.getIP();
		} catch (IOException e) {
			return null;
		}
	}

	private static Server chooseCloest(Vector<Server> serverCollection,
			Location location) {
		double cloestDistance = Double.POSITIVE_INFINITY;
		Server cloestServer = null;
		for (int i = 0; i < serverCollection.size(); i++) {
			Server server = serverCollection.get(i);
			double sLat = server.getLocation()[0];
			double sLng = server.getLocation()[1];
			double distanceToServer = computeDistance(sLat, sLng,
					location.getLatitude(), location.getLongitude());
			if (distanceToServer < cloestDistance) {
				cloestDistance = distanceToServer;
				cloestServer = server;
			}
		}
		return cloestServer;
	}

	private static double computeDistance(double sLat, double sLng,
			Double latitude, Double longitude) {
		double radLat1 = rad(sLat);
		double radLat2 = rad(latitude);
		double a = radLat1 - radLat2;
		double b = rad(sLng) - rad(longitude);
		double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
				+ Math.cos(radLat1) * Math.cos(radLat2)
				* Math.pow(Math.sin(b / 2), 2)));
		s = s * EARTH_RADIUS;
		s = Math.round(s * 10000) / 10000;
		return s;
	}

	private static double rad(double d) {
		return d * Math.PI / 180.0;
	}

	public static Server getBestServer(String hostAddress) {
		// TODO Auto-generated method stub
		return null;
	}

	public static long getTTL() {
		return TTL;
	}
}

