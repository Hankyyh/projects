package edu.usc.bg.cache.coordinator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class BGCacheCoordinatorServer
{
	public static Properties props = new Properties();
	public static ConcurrentHashMap<String, LinkedHashSet<InetAddress>> invalidationMap = new ConcurrentHashMap<>();

	public static void fetchProperties()
	{
		props.setProperty("portnumber", "7654");
	}
	
	public static void runServer() throws IOException
	{
		ServerSocket serverSocket = null;
		
		boolean listeningSocket = true;

        try 
        {
            serverSocket = new ServerSocket(new Integer(props.getProperty("portnumber")));
        } 
        catch (IOException e) 
        {
            System.err.println("Could not listen on port: "+props.getProperty("portnumber"));
        }

        while(listeningSocket)
        {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Accepted Connection....");
            CacheCoordinatorThread serverThread = new CacheCoordinatorThread(clientSocket);
            //serverThread.setPriority(Thread.MAX_PRIORITY);
            serverThread.start();
        }
        serverSocket.close();
	}
	
	public static void main(String args[])
	{
		BGCacheCoordinatorServer.fetchProperties();
		
		try
		{
			System.out.println("Server Started...");
			BGCacheCoordinatorServer.runServer();
		}
		catch(IOException ioe)
		{
			System.out.println("Error Closing Server Socket:"+ioe.getMessage());
		}
	}
}
