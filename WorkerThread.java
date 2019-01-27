import java.io.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import java.lang.*;

public class WorkerThread extends Thread{
	
	private Socket connection;
	public Socket socketFromWebServer;
	public String answerCode_str;
	public int localPort;
	
	public int num_byte_read = 0;
	public int off = 0;
	public int counter;
	public File newDir, currentDir;
	public String http_response_header_string;	
	public String[] ContentLengthPart_front;
	public String[] ContentLengthPart_back;
	public String lengthTemp;
	public String ContentLength_string;
	public long objectLength;
	
	
	
	public WorkerThread(Socket socket) {
		socketFromWebServer = socket;
	}
	
	public void run () {
		String s;
		//open the server socket
		//set socket timeout option using setSoTimeout(1000)
		try {
				//accept new connection
				PrintWriter outputStream = new PrintWriter (new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
				Scanner inputStream = new Scanner(connection.getInputStream(), "UTF-8");
				while (true) {
					s = inputStream.nextLine();
					
					//process stream info + download stuff
					getObject(s, localPort);
					//
					System.out.println(">> " + s);
					outputStream.flush();
				}
				//close connection
				//send "connection:close" to server response
			} catch (Exception e) {
				//do nothing, this is OK
				//allows the process to check the shutdown flag
			}

	}
	
	/**
	 * Separates the path such that the necessary folders can be created and creates
	 * the path directory for the future file.
	 *
	 * @param newLastPath 	name of the path/files
	 * @param oldDirectoryName	name of the directory where the folders should be created
	 * @param noSourceDirectory 	Indication whether or not a source directory exists or not 
	 */
	public static File folderBreakupAndMake (String newLastPath, File oldDirectoryName, Boolean noSourceDirectory) {
		//Separates such that it knows how many subdirectories need to be created
		newLastPath = newLastPath.replace("\"", "");
		String[] splitAgain = newLastPath.split("/");
		String newDirectoryName = "";
		
		//runs if the last element does NOT contain "." in it
		if (!splitAgain[splitAgain.length-1].contains(".")) {
			for (int i = 0; i < splitAgain.length; i++) {
				newDirectoryName = splitAgain[i];
				//This assumes that we are making the folder in the primary directory
				if (noSourceDirectory == true) {
					File newDir = new File(newDirectoryName);
					if (!newDir.exists()) {
						newDir.mkdir();
					}
					noSourceDirectory = false;
					oldDirectoryName = newDir;
				} else {
					//Creates a new directory given that it is within another newly-created directory
					File newDir = new File(oldDirectoryName, newDirectoryName);
					newDir.mkdir();
					oldDirectoryName = newDir;
				}
			}
		//runs in all other cases
		} else {
			for (int i = 0; i < splitAgain.length-1; i++) {
				newDirectoryName = splitAgain[i];
				if (noSourceDirectory == true) {
					File newDir = new File(newDirectoryName);
					if (!newDir.exists()) {
						newDir.mkdir();
					}
					noSourceDirectory = false;
					oldDirectoryName = newDir;
				} else {
					//Creates a new directory given that it is within another newly-created directory
					File newDir = new File(oldDirectoryName, newDirectoryName);
					newDir.mkdir();
					oldDirectoryName = newDir;
				}
			}
		}
		newLastPath = splitAgain[splitAgain.length-1];
		newDirectoryName = newDirectoryName.replace("\\", "\\\\");
		return oldDirectoryName;
	}
	
		/**
	 *
	 *
	 * @param url 	the url of which is given by retrieving lines
	 * @param port	the port that was given at the beginning, in the event that a port is not provided
	 */
	public void getObject(String url, int portN) {
		String [] URLparts = url.split("/",2);
		String host, port, filepath;
		if(URLparts[0].contains(":")) {
			String [] hostandport = URLparts[0].split(":",2);
			host = hostandport[0];
			port = hostandport[1];
			filepath = "/" + URLparts[1];
			url = url = hostandport[0] + "/" + URLparts[1];
		} else {
			host = URLparts[0];
			port = Integer.toString(portN);
			filepath = "/" + URLparts[1];
		}
		int port_int = Integer.parseInt(port);
		
		String requestLine_1 = "GET " + filepath + " HTTP/1.0\r\n";
		String eoh_line = "\r\n";
		try {
			String http_header = requestLine_1 + eoh_line; 
			byte[] http_header_in_bytes = http_header.getBytes("US-ASCII");
			socketFromWebServer.getOutputStream().write(http_header_in_bytes);
			byte[] http_response_header_bytes = new byte[2048];
			byte[] http_object_bytes = new byte[1048];
			try {
				while (num_byte_read != -1) {
					socketFromWebServer.getInputStream().read(http_response_header_bytes, off, 1);
					off++;
					http_response_header_string = new String(http_response_header_bytes, 0, off, "US-ASCII");
					if (http_response_header_string.contains("\r\n\r\n")) {
						break;
					}
				}
			} catch (IOException e) {
				System.out.println("Error: " + e.getMessage());
			}
			String headerResponse;
			if (http_response_header_string.contains("200 OK")){
				System.out.println("200 OK");
				//run below-below code on downloading file.
				//get object size from the header and save as integer objectLength
				ContentLengthPart_front = http_response_header_string.split("Content-Length: ",2);
				lengthTemp = ContentLengthPart_front[1];
				ContentLengthPart_back = lengthTemp.split("\r",2);
				ContentLength_string = ContentLengthPart_back[0];
				int objectLength_int = Integer.parseInt(ContentLength_string);
				Long objectLength = new Long(objectLength_int);
					try {
						currentDir = folderBreakupAndMake(url, newDir, false); //make directories as necessary
						//creates new file inside this directory				
						File file = new File(url);
						InputStream inStream = socketFromWebServer.getInputStream();
						ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream();
						FileOutputStream fileOutStream = new FileOutputStream(file);
						while (num_byte_read != -1) {
							if (counter == objectLength) {
								break; //aka we don't read anymore
							}
							num_byte_read = socketFromWebServer.getInputStream().read(http_object_bytes);
							//System.out.println("NumBytesRead = " + num_byte_read);
							byteArrayOutStream.write(http_object_bytes, 0, num_byte_read);
							fileOutStream.write(http_object_bytes);		// write to file 'num_byte_read' bytes
							fileOutStream.flush();
							counter += num_byte_read;  // 'counter' incremented to total number of bytes read			
							
						}
						//closing required streams
						byteArrayOutStream.close();
						fileOutStream.close();
						inStream.close();
					} catch (IOException e) {
						//error in downloading file
						System.out.println("Error: " + e.getMessage());
					}
					//outputStream.flush();
			} else if (http_response_header_string.contains("400 Bad Request")) {
				System.out.println("Bad request");
			} else if (http_response_header_string.contains("404 Not Found")) {
				System.out.println("Not found.");
			}
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
		}
	}
	
	public void setPort (int port) {
		localPort = port;
	}
	
}