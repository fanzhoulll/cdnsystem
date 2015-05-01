package DNSServer;

import java.util.*;
import java.util.Map.Entry;

public class DNSResponse {
	
	private LinkedHashMap<String, byte[]> response = new LinkedHashMap<String, byte[]>();
	
	public DNSResponse()
	{
		response.put("ID", null);
		response.put("FLAG", null);
		response.put("QDCOUNT", null);
		response.put("ANCOUNT", null);
		response.put("NSCOUNT", null);
		response.put("ARCOUNT", null);
		response.put("QNAME", null);
		response.put("QTYPE", null);
		response.put("QCLASS", null);
		response.put("NAME", null);
		response.put("TYPE", null);
		response.put("CLASS", null);
		response.put("TTL", null);
		response.put("RDLENGTH", null);
		response.put("RDATA", null);
	}
	
	public void setID(byte[] idToSend) {
		response.put("ID", idToSend);
	}
	
	public void setFlag(byte[] flag) {
		response.put("FLAG", flag);
	}
	public void setQDCount(byte[] qdCount){
		response.put("QDCOUNT", qdCount);
	}
	public void setANCount(byte[] anCount) {
		response.put("ANCOUNT", anCount);
	}

	public void setNSCount(byte[] nsCount) {
		response.put("NSCOUNT", nsCount);
	}

	public void setARCount(byte[] arCount) {
		response.put("ARCOUNT", arCount);
	}

	public void setName(byte[] questionInByteArray) {
		response.put("NAME", questionInByteArray);
	}

	public void setType(byte[] Type) {
		response.put("TYPE", Type);
	}

	public void setClass(byte[] Class) {
		response.put("CLASS", Class);
	}

	public void setQName(byte[] name) {
		response.put("QNAME", name);
	}

	public void setQtype(byte[] qtype) {
		response.put("QTYPE", qtype);
	}

	public void setQClass(byte[] qclass) {
		response.put("QCLASS", qclass);
	}

	public void setTTL(byte[] ttl) {
		response.put("TTL", ttl);
	}
	
	public void setRDlength(byte[] rDlength) {
		response.put("RDLENGTH", rDlength);
	}

	public void setRData(byte[] selectedIP) {
		response.put("RDATA", selectedIP);
	}
	
	public byte[] getResponse() {
		byte[] responseInByteArray = new byte[0];
		for (Entry<String, byte[]> entry : response.entrySet()) {
		    byte[] value = entry.getValue();
		    responseInByteArray = Transform.append(responseInByteArray, value);
		}
		return responseInByteArray;
	}

	


}
