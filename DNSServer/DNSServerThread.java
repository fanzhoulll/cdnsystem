package DNSServer;

import java.io.*;
import java.net.*;

public class DNSServerThread extends Thread {

	// private int serverPort = 0;
	private DatagramSocket socket = null;
	private DatagramPacket packet = null;
	private String QUESTIONACCEPTED;
	private DNSResponse response;
	private String responseIP;
	private long TTL;
	
	public DNSServerThread(DatagramSocket socket, DatagramPacket packet,
			String acceptQuestion) {
		QUESTIONACCEPTED = acceptQuestion;
		this.socket = socket;
		this.packet = packet;
	}

	public void run() {
		try {
			InetAddress sourceAddress = packet.getAddress();
			int sourcePort = packet.getPort();
			ParseDNSRequest parse = new ParseDNSRequest(packet);
			String question = parse.getQuestionInString();
			if (!question.equals(QUESTIONACCEPTED)) {
				System.out.println("Unaccepted query: " + question);
				return;
			}
			response = new DNSResponse();
			responseIP = CDNSystem.getBestServerIP(sourceAddress.getHostAddress());
			
			
			TTL = CDNSystem.getTTL();
			constructResponse(parse);
			byte[] buf_send = response.getResponse();
			if (buf_send == null) {
				System.out.println("Fail to construct response !");
				return;
			}
			packet = new DatagramPacket(buf_send, buf_send.length,
					sourceAddress, sourcePort);
			socket.send(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Fail to send back dns response to "
					+ packet.getAddress().getHostName());
		}
	}

	private void constructResponse(ParseDNSRequest parse) {
		response.setID(parse.getIdToSend());
		response.setFlag(parse.getFlag());
		response.setQDCount(parse.getQDCount());
		response.setANCount(parse.getANCount());
		response.setNSCount(parse.getNSCount());
		response.setARCount(parse.getARCount());
		byte[] qname = parse.getQuestionInByteArray();
		byte[] qtype = parse.getQType();
		byte[] qclass = parse.getQClass();
		response.setQName(qname);
		response.setQtype(qtype);
		response.setQClass(qclass);
		response.setName(qname);
		response.setType(qtype);
		response.setClass(qclass);
		response.setRDlength(parse.getRDlength());
		response.setTTL(Transform.longToByteArray(TTL, 4));
		response.setRData(Transform.addressToByteArray(responseIP));
	}

}