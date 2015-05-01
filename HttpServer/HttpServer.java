package httpServer;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class HttpServer
{
	private ConcurrentHashMap<String, Integer> cacheRecord;
	// private ConcurrentHashMap<String, Double> clientRecord = new ConcurrentHashMap<String, Double>(); // IP->Delay
	// (in ms)
	private String CachePath = "./Cache/";
	private static InetSocketAddress originSockaddr;

	private int getViewCount(String fileName)
	{
		File viewRecord = new File("./view_record");
		BufferedReader br;
		try
		{
			br = new BufferedReader(new FileReader(viewRecord));
			String line;
			while ((line = br.readLine()) != null)
			{
				String delims = "[ ]+";
				String[] tokens = line.split(delims);
				String name = tokens[0];
				int viewCount = Integer.parseInt(tokens[1]);
				if (fileName.equals(name))
				{
					br.close();
					return viewCount;
				}
			}
		}
		catch (IOException e)
		{
			System.out.println("Fail to read view count record!");
		}
		return 0;
	}

	public ArrayList<String> getFileNameList(String path)
	{
		File fileFolder = new File(path);
		if (fileFolder.exists())
		{
			ArrayList<String> list = new ArrayList<String>();
			File[] files = fileFolder.listFiles();
			for (File file : files)
			{
				if (!file.isDirectory())
				{
					list.add(file.getName());
				}
			}
			return list;
		}
		else
		{
			return null;
		}
	}

	private void initCache()
	{
		File directoryOfCache = new File(CachePath);
		// configFile = new File(CachePath + CacheConfig);
		if (!directoryOfCache.exists())
		{
			// If none of cache file exist, the cache record is also empty
			directoryOfCache.mkdir();
		}
		else
		{
			ArrayList<String> fileNameList = getFileNameList(CachePath);
			for (String cacheFile : fileNameList)
			{
				int viewCount = getViewCount(cacheFile);
				cacheRecord.put(cacheFile, viewCount);
			}
			/*
			 * Iterator<String> it = cacheRecord.keySet().iterator();
			 * 
			 * 
			 * while(it.hasNext()){ String key = it.next(); System.out.println(key + ":" + cacheRecord.get(key)); }
			 */

		}
		/*
		 * if (!configFile.exists()) { try { configFile.createNewFile(); } catch (IOException e) {
		 * System.out.println("Fail to create cache config file!"); } }
		 */
	}

	public HttpServer(int port, SocketAddress sockaddr)
	{
		try
		{
			System.out.println("Wait for connection on port " + port + "....");
			@SuppressWarnings("resource")
			ServerSocket httpServer = new ServerSocket(port);
			cacheRecord = new ConcurrentHashMap<String, Integer>();
			initCache();
			while (true)
			{
				Socket socket = httpServer.accept();
				// System.out.println("Look who is here! " + socket.getRemoteSocketAddress());
				new httpServerThread(socket);
			}
		}
		catch (IOException e)
		{
			System.out.println("Server error!");
		}
	}

	// --- CreateServerThread
	class httpServerThread extends Thread
	{
		private Socket threadSocket;
		private String fileAddress;
		private String fileName;
		private String filePath;
		// private String CacheConfig = "cache.config";
		public static final long SECOND = 1000;
		public static final long CACHELIMIT = 10000000; // In byte

		public httpServerThread(Socket socket)
		{
			threadSocket = socket;
			String timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
			System.out.println("Time: " + timeStamp + " Request from: " + threadSocket.getInetAddress().getHostName());
			start();
		}

		// This function will only be invoked when cache space is not enough
		// It is possible that when you try to read cacheconfig, there are other
		// threads that are try to read/write
		// the same file, so add lock when you try to write it.
		/*
		 * private void cleanCache() { try { BufferedReader br = new BufferedReader(new FileReader(CacheConfig)); String
		 * line; while ((line = br.readLine()) != null) { // process the line. } } catch (IOException e) {
		 * System.out.println("Fail to create cache directory!"); } }
		 */

		public void run()
		{
			try
			{
				BufferedReader reader = new BufferedReader(new InputStreamReader(threadSocket.getInputStream()));
				String command = reader.readLine();
				// String filePath = getPath(command);
				if (command.subSequence(0, 2).equals("IP"))
				{
					sendDelayRecord(command.substring(2));
					return;
				}
				filePath = getFilePath(command);
				if (filePath == null)
					return;
				fileAddress = originSockaddr.getHostString() + filePath;
				fileName = getNameFromPath(filePath);
				File fileToSent = new File(CachePath + fileName); // file asked by client
				// In case we need to fetch it. filePage = HTTP Header + HTTP Body
				byte[] fileByte = null;
				// File does not exist, need to fetch it
				if (!cacheRecord.containsKey(fileName))
				{
					// return input stream
					fileByte = getFile(filePath);
				}
				else
				{
					// File does exist in cache, read first line to make sure it is the file you need
					BufferedReader cacheContent = new BufferedReader(new FileReader(CachePath + fileName));
					String cacheAddress = cacheContent.readLine(); // First line
					cacheContent.close();
					// Check if the cache address is equal to the file address
					// in case different file that has same file name
					if (fileAddress.equals(cacheAddress))
					{
						// including address
						fileByte = readFileToByteArray(fileToSent);
						// Get rid of address
						fileByte = parseCache(fileByte);
					}
					else
					{
						fileByte = getFile(filePath);
					}
				}
				OutputStream out = threadSocket.getOutputStream();
				if (fileByte == null)
				{
					System.out.println("Fail to get input stream!");
				}
				else
				{
					out.write(fileByte);
					out.close();
					// if (getResponseCode(fileByte) != 404)
					HandleNewCache(fileByte);
				}
				// HandleNewClient();
				threadSocket.close();
			}
			catch (IOException e)
			{
				System.out.println("Server error!");
			}
		}

		private void sendDelayRecord(String clientIP)
		{
			try
			{
				Writer writer = new OutputStreamWriter(threadSocket.getOutputStream());
				Double delayToClient = pingDelay(clientIP);
				Double delayToOrigin = estimateHTTPDelay(originSockaddr);
				Double totalDelay;
				if (delayToClient != 0 && delayToOrigin != 0)
					totalDelay = delayToClient + delayToOrigin;
				else
					totalDelay = 0.0;
				InetAddress clientAddress = InetAddress.getByName(clientIP);
				System.out.println("To client " + clientAddress.getHostName() + ": " + delayToClient + " To origin server: " + delayToOrigin + " Total delay: "
						+ totalDelay);
				writer.write(totalDelay.toString() + "\n");
				writer.write("\n");
				writer.flush();
				writer.close();
				threadSocket.close();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private Double estimateHTTPDelay(InetSocketAddress address)
		{
			try
			{
				Socket client = new Socket();
				client.connect(address, 1000);
				Writer writer = new OutputStreamWriter(client.getOutputStream());
				writer.write("GET /x HTTP/1.1\r\n");
				writer.write("Host: " + address.getHostString() + "\r\n");
				writer.write("Connection: close\r\n");
				writer.write("\r\n");
				writer.flush();
				// The inputstream is temporal, will no longer exist after this socket is close
				// so we need to clone it to clientInput
				long startTime = System.currentTimeMillis();
				byte[] buffer = new byte[2048];
				int len = -1;
				InputStream temInPutStream = client.getInputStream();
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				while ((len = temInPutStream.read(buffer)) != -1)
				{
					out.write(buffer, 0, len);
				}
				out.flush();
				writer.close();
				client.close();
				long endTime = System.currentTimeMillis();
				double timeDifference = endTime - startTime;
				return timeDifference;
			}
			catch (IOException e)
			{
				System.out.println("Fail to estimate delay to " + address.getHostName());
				return 0.0;
			}
		}

		private double pingDelay(String ip)
		{
			Runtime run = Runtime.getRuntime();
			String cmd = "ping -c 1 -W 1 " + ip;
			try
			{
				Process pWget = run.exec(cmd);
				// System.out.println(cmd);
				BufferedInputStream in = new BufferedInputStream(pWget.getInputStream());
				BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
				String returnMessage = "";
				String lineStr;
				while ((lineStr = inBr.readLine()) != null)
					returnMessage += lineStr;
				if (pWget.waitFor() != 0)
				{
					if (pWget.exitValue() == 1)
						return 0;
				}
				inBr.close();
				in.close();
				return getDelay(returnMessage);
			}
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				return 0;
			}
		}

		private double getDelay(String returnMessage)
		{
			int beginIndex = returnMessage.indexOf("rtt min/avg/max/mdev = ") + 23;
			int endIndex = returnMessage.indexOf("/");
			String rttInformation = returnMessage.substring(beginIndex);
			endIndex = rttInformation.indexOf("/");
			String meanRTT = rttInformation.substring(0, endIndex);
			return Double.parseDouble(meanRTT);
		}

		/*
		 * private int getResponseCode(byte[] fileByte) { String stringContent = new String(fileByte); String response =
		 * stringContent.substring(9, 12); return Integer.parseInt(response); }
		 */

		private byte[] parseCache(byte[] fileByte)
		{
			byte[] newArray;
			int index = 0;
			for (index = 0; index < fileByte.length; index++)
			{
				if ((char) fileByte[index] == '\n')
					break;
			}
			// +1 because of extra '\n'
			index = index + 1;
			newArray = new byte[fileByte.length - index];
			for (int i = 0; i < fileByte.length - index; i++)
			{
				newArray[i] = fileByte[i + index];
			}
			return newArray;
		}

		private byte[] readFileToByteArray(File file)
		{
			FileInputStream fileInputStream = null;
			byte[] bFile = new byte[(int) file.length()];
			try
			{
				// convert file into array of bytes
				fileInputStream = new FileInputStream(file);
				fileInputStream.read(bFile);
				fileInputStream.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			return bFile;
		}

		private String getNameFromPath(String filePath)
		{
			if (!filePath.equals("/"))
			{
				String delim1 = "/+";
				String[] pathSection = filePath.split(delim1);
				return pathSection[pathSection.length - 1];
			}
			else
			{
				return "index.html";
			}
		}

		private void HandleNewCache(byte[] fileByte)
		{
			File newCache = new File(CachePath + fileName);
			try
			{
				if (newCache.exists())
				{
					// Two possibilities (1) Other threads have create the
					// cache, or Overwrite
					BufferedReader cacheContent = new BufferedReader(new FileReader(CachePath + fileName));
					String cacheAddress = cacheContent.readLine(); // First line
					cacheContent.close();
					if (fileAddress.equals(cacheAddress))
					{
						return;
					}
				}

				// There is actually better way to handle this
				// Set boolean, doAddNew
				// while folderSize(new File(CachePath)) + newCache.length() >
				// CACHELIMIT
				// if countNewCache == -1, doAddNew = false, break, no need to
				// waste time else find smallest
				// if smallest > countNewCache, doAddNew = false, break, do
				// nothing
				// if smallest < countNewCache, doAddNew = true, delete that
				// smallest file and record, loop
				// if (doAddNew), add cache
				int countNewCache = getViewCount(fileName);
				boolean addNewCache = true;
				File cacheFolder = new File(CachePath);
				if (!cacheFolder.exists())
				{
					System.out.println("What the hell??");
					return;
				}
				long cacheTotalSize = folderSize(cacheFolder);
				if (cacheTotalSize == -1)
				{
					System.out.println("Do not have permission to operate cache folder");
					return;
				}
				while (cacheTotalSize + fileByte.length > CACHELIMIT)
				{
					if (countNewCache == 0 || newCache.length() > CACHELIMIT)
					{
						addNewCache = false;
						break;
					}
					else
					{
						Entry<String, Integer> lowestViewRecord = getLowestRecord();
						if (lowestViewRecord == null)
							return;
						if (lowestViewRecord.getValue() > countNewCache)
						{
							addNewCache = false;
							break;
						}
						else
						{
							String lowestViewCache = lowestViewRecord.getKey();
							File lowestViewFile = new File(CachePath + lowestViewCache);
							if (lowestViewFile.delete())
							{
								System.out.println(lowestViewFile.getName() + " is deleted!");
							}
							else
							{
								// Maybe it has been deleted by other threads
								System.out.println("Fail to delete cache file " + lowestViewFile.getName());
							}
							cacheRecord.remove(lowestViewCache);
						}
					}
				}
				if (addNewCache)
				{
					cacheRecord.put(fileName, countNewCache);
					OutputStream out = new FileOutputStream(CachePath + fileName);
					String addressString = fileAddress + "\n";
					byte[] addressArray = addressString.getBytes();
					out.write(addressArray, 0, addressArray.length);
					out.write(fileByte);
					out.close();
				}
			}
			catch (IOException e)
			{
				System.out.println("Fail to write cache file: " + filePath);
			}
		}

		private Entry<String, Integer> getLowestRecord()
		{
			Entry<String, Integer> min = Collections.min(cacheRecord.entrySet(),
					new Comparator<Entry<String, Integer>>()
					{
						public int compare(Entry<String, Integer> entry1, Entry<String, Integer> entry2)
						{
							return entry1.getValue().compareTo(entry2.getValue());
						}
					});
			return min;
		}

		private long folderSize(File cachePath)
		{
			long length = 0;
			File[] fineNameList = cachePath.listFiles();
			if (fineNameList == null)
			{
				return -1;
			}
			else if (fineNameList.length > 0)
			{
				for (File file : fineNameList)
				{
					if (file.exists() && file.isFile())
						length += file.length();
					else
						length += folderSize(file);
				}
				return length;
			}
			else
				return 0;
		}

		/*
		 * private String readFileToString(String fileName) { try { BufferedReader br = new BufferedReader(new
		 * FileReader(fileName)); StringBuilder sb = new StringBuilder(); String line = br.readLine(); while (line !=
		 * null) { sb.append(line); sb.append("\n"); line = br.readLine(); } br.close(); return sb.toString(); } catch
		 * (IOException e) { System.out.println("Read Fail!!"); return null; } }
		 */

		private byte[] getFile(String filePath)
		{
			try
			{
				Socket client = new Socket();
				client.connect(originSockaddr, 1000);
				Writer writer = new OutputStreamWriter(client.getOutputStream());
				writer.write("GET " + filePath + " HTTP/1.1\r\n");
				writer.write("Host: " + originSockaddr.getHostString() + "\r\n");
				writer.write("Connection: close\r\n");
				writer.write("\r\n");
				writer.flush();
				// The inputstream is temporal, will no longer exist after this socket is close
				// so we need to clone it to clientInput
				long startTime = System.currentTimeMillis();
				byte[] buffer = new byte[2048];
				int len = -1;
				try
				{
					InputStream temInPutStream = client.getInputStream();
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					while ((len = temInPutStream.read(buffer)) != -1)
					{
						out.write(buffer, 0, len);
					}
					out.flush();
					long endTime = System.currentTimeMillis();
					System.out.println("Time difference is: " + (endTime - startTime) + "ms");
					return out.toByteArray();
				}
				catch (IOException e)
				{
					System.out.println("Clone input stream fail!");
				}
				writer.close();
				client.close();
			}
			catch (Exception e)
			{
				System.out.println("Cannot get connect to address: " + originSockaddr.getHostName());
			}
			return null;
		}

		private String getFilePath(String command)
		{
			String delim1 = "[ ]+";
			String[] cmdSection = command.split(delim1);
			return cmdSection[1];
		}
	}

	public static void main(String[] args)
	{
		if (!ifArgumentValid(args))
		{
			System.out.println("Usage: ./httpserver -p <port> -o <origin>");
			System.out.println("Parameter: port: 40000-65535, origin: address of origin server");
			return;
		}
		int port = Integer.parseInt(args[1]);
		String originServer = args[3];
		String originHost = getHostByAddress(originServer);
		int originPort = getPortByAddress(originServer);
		if (originPort < 0)
		{
			System.out.println("You did not specify original server port. Using default port 8080");
			originSockaddr = new InetSocketAddress(originHost, 8080);
		}
		else
		{
			originSockaddr = new InetSocketAddress(originHost, originPort);
		}

		new HttpServer(port, originSockaddr);
	}

	private static int getPortByAddress(String websiteAddress)
	{
		websiteAddress = websiteAddress.replace("http://", "");
		websiteAddress = websiteAddress.replace("https://", "");
		websiteAddress = websiteAddress.replace("www.", "");
		int startIndex = websiteAddress.indexOf(":");
		int endIndex = websiteAddress.indexOf("/");
		if (endIndex == -1)
			endIndex = websiteAddress.length();
		String portInString = websiteAddress.substring(startIndex + 1, endIndex);
		if (portInString.matches("^-?\\d+$"))
			return Integer.parseInt(portInString);
		else
			return -1;
	}

	private static String getHostByAddress(String websiteAddress)
	{
		websiteAddress = websiteAddress.replace("http://", "");
		websiteAddress = websiteAddress.replace("https://", "");
		websiteAddress = websiteAddress.replace("www.", "");
		int index1 = websiteAddress.indexOf("/"); // Identify path
		int index2 = websiteAddress.indexOf(".com:"); // Identify port
		if (index2 != -1)
		{
			websiteAddress = websiteAddress.substring(0, index2 + 4);
		}
		else if (index1 != -1)
			websiteAddress = websiteAddress.substring(0, index1);
		return websiteAddress;
	}

	private static boolean ifArgumentValid(String[] args)
	{
		if (args.length != 4)
			return false;
		if (!args[0].equals("-p"))
			return false;
		if (!args[1].matches("^-?\\d+$"))
			return false;
		if (!args[2].equals("-o"))
			return false;
		return true;
	}
}
