package DNSServer;

import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;

class ParseDNSRequest {

	private int len;
	private byte dataTrimmed[];

	public ParseDNSRequest(DatagramPacket packet) {
		this.len = packet.getLength();
		dataTrimmed = trimData(packet.getData(), len); // Removing unnecessary zeroes
	}

	byte[] getIdToSend() {

		byte[] idReady = new byte[2];
		for (int i = 0; i < 2; i++) {
			idReady[i] = dataTrimmed[i];
		}
		return idReady;
	}
 
	public String getQuestionInString() {
		int t = 0;
		byte[] dnsBody = new byte[len - 12];
		int j = 0;
		for (int i = 12; i < dataTrimmed.length; i++) {
			if (dataTrimmed[i] == 0 & dataTrimmed[i + 1] == 0
					& dataTrimmed[i + 2] == 1) {
				t = i;
				break;
			}
			dnsBody[j] = dataTrimmed[i];
			j++;
		}
		int i = 0;
		byte[] questionForCheck = new byte[(t - 12)];
		for (i = 0; i < (t - 12); i++) {
			questionForCheck[i] = dnsBody[i];
		}
		String questionInString;
		try {
			questionInString = new String(questionForCheck, "UTF-8");
			questionInString = questionInString.replaceAll("[^\\x20-\\x7e]", ".");
			questionInString = questionInString.substring(1);
			return questionInString;
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	
	public byte[] getQuestionInByteArray() {
		int t = 0;
		byte[] dnsBody = new byte[len - 12];
		int j = 0;
		for (int i = 12; i < dataTrimmed.length; i++) {
			if (dataTrimmed[i] == 0 & dataTrimmed[i + 1] == 0
					& dataTrimmed[i + 2] == 1) {
				t = i;
				break;
			}
			dnsBody[j] = dataTrimmed[i];
			j++;
		}
		byte[] question = new byte[(t - 12) + 1];
		int i = 0;
		for (i = 0; i < (t - 12); i++) {
			question[i] = dnsBody[i];
		}
		question[i] = 0;
		return question;
	}

	private byte[] trimData(byte[] data2, int len2) { // Removing unnecessary
														// zeroes

		byte[] data3 = new byte[len2];
		for (int i = 0; i < len2; i++) {
			data3[i] = data2[i];
		}
		return data3;
	}

	public String getID(byte[] data2) {

		String id = Transform.byteArrayToHexadecimal(data2);
		char[] ch = id.toCharArray();
		String sid;
		char[] ch1 = new char[5];
		for (int i1 = 0; i1 < 5; i1++) {
			ch1[i1] = ch[i1];
		}
		sid = new String(ch1);
		sid = sid.replaceAll("\\s+", ""); 
		return sid;
	}

	public byte[] getFlag()  {
		byte[] bitArrayToSend = new byte[2];
		int j = 2;
		for (int i = 0; i < 2; i++) {
			bitArrayToSend[i] = dataTrimmed[j];
			j++;
		}
		byte readyBit[] = Transform.arrayByteToBit(bitArrayToSend);

		readyBit[0] = 1;
		byte readyByte[] = Transform.arrayBitToByte(readyBit);
		return readyByte;
	}

	public byte[] leftOverPacketContent() {
		byte[] leftOver = new byte[4];
		int j = 8;
		for (int i = 0; i < 4; i++) {
			leftOver[i] = dataTrimmed[j];
			j++;
		}
		return leftOver;
	}

	public byte[] addQuestion() {
		int questionLen = len - 12;
		byte[] readyQuestion = new byte[questionLen];

		int j = 12;
		for (int i = 0; i < questionLen; i++) {
			readyQuestion[i] = dataTrimmed[j];
			j++;
		}
		return readyQuestion;
	}

	public byte[] getQDCount() {
		byte[] QDCount = new byte[2];
		int j = 4;
		for (int i = 0; i < 2; i++) {
			QDCount[i] = dataTrimmed[j];
			j++;
		}
		return QDCount;
	}

	public byte[] getANCount() {
		byte[] ANCount = new byte[2];
		int j = 6;

		for (int i = 0; i < 2; i++) {
			ANCount[i] = dataTrimmed[j];
			j++;
		}
		byte[] arrayBit = Transform.arrayByteToBit(ANCount);
		arrayBit[arrayBit.length - 1] = 1;
		ANCount = Transform.arrayBitToByte(arrayBit);
		return ANCount;
	}

	public byte[] getQType() {
		byte[] QType = new byte[2];
		byte[] arrayBit = Transform.arrayByteToBit(QType);
		arrayBit[arrayBit.length - 1] = 1;
		QType = Transform.arrayBitToByte(arrayBit);
		return QType;
	}

	public byte[] getQClass() {
		byte[] QClass = new byte[2];
		byte[] arrayBit = Transform.arrayByteToBit(QClass);
		arrayBit[arrayBit.length - 1] = 1;
		QClass = Transform.arrayBitToByte(arrayBit);
		return QClass;
	}

	public byte[] getNSCount() {
		byte[] nscount = new byte[2];
		int j = 8;
		for (int i = 0; i < 2; i++) {
			nscount[i] = dataTrimmed[j];
			j++;
		}
		return nscount;
	}

	public byte[] getARCount() {
		byte[] arcount = new byte[2];
		int j = 10;
		for (int i = 0; i < 2; i++) {
			arcount[i] = dataTrimmed[j];
			j++;
		}
		return arcount;
	}

	public byte[] getRDlength() {
		byte[] IPLength = new byte[2];
		IPLength[1] = 4;
		return IPLength;
	}

}