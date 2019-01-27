/**
 * Multithreading HTTP 1.0 TCP connections
 * 
 * @author 	Yin-Li (Emily) Chow
 * @version	1.0, Oct. 27th, 2016
 *
 */
import java.io.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;

public class WebServer extends Thread{
	
	private volatile boolean shutdown = false;
	public int localPort;
	
	/**
	 *
	 * @param port	the port number that will be used for opening the socket
	*/
	public WebServer(int port) {
		setPort(port);
		
	}
	
	public void run () {
		ServerSocket serverSocket;
		try {	
			serverSocket = new ServerSocket(localPort);
			while (true) {
				try {
					Socket socket = serverSocket.accept();
					WorkerThread tcpConnection = new WorkerThread(socket);
					//new Thread().start();
					tcpConnection.start();
				} catch (Exception e) {
					System.out.println("Error: " + e.getMessage());
				}
			}
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
		}
	}
	
	public void setPort (int port) {
		localPort = port;
	}
	
	public void shutdown() {
		shutdown = true;
	}
	
}