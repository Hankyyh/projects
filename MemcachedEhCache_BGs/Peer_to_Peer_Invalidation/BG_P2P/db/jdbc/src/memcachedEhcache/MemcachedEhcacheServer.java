package memcachedEhcache;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.ehcache.Cache;


public class MemcachedEhcacheServer extends Thread
{
	static ServerSocket serverSocket = null;
	static boolean listeningSocket = true;

	public MemcachedEhcacheServer(String portNumber)
	{
        try 
        {
        	if(serverSocket==null)
        	{
				serverSocket = new ServerSocket(new Integer(portNumber));
				System.out.println("Server Socket Initialized....");
        	}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void run()
	{
		System.out.println("Running MemcachedEhCache Server for Invalidation...");
		
        try 
        {
            while(listeningSocket)
            {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted an invalidation request....");
                MemcachedEhcacheServerThread serverThread = new MemcachedEhcacheServerThread(clientSocket);
                serverThread.start();
            }
        } 
        catch (IOException e) 
        {
            System.err.println("Error while accepting from serverSocket: "+e.getMessage());
        }
	}
	
	public void cleanUp()
	{
		 try 
		 {
			 System.out.println("Stopping Invalidation Server...");
			 listeningSocket=false;
			serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
