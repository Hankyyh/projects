package edu.usc.bg.cache.coordinator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedHashSet;

import edu.usc.bg.memcached.coordinator.RemoteMemcachedClient;

public class CacheCoordinatorThread extends Thread
{
    private Socket socket = null;
    RemoteMemcachedClient cacheclient;
    DataInputStream dis;
    DataOutputStream dos;

    public CacheCoordinatorThread(Socket socket) 
    {
        super("CacheCoordinatorThread");
        this.socket = socket;
        
        InputStream in=null;
	    OutputStream out=null; 
	    
		try 
		{
			in  = socket.getInputStream();
			out = socket.getOutputStream(); 
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		dis = new DataInputStream(in);
	    dos = new DataOutputStream(out);
        
        cacheclient = new RemoteMemcachedClient();
        cacheclient.init();
    }
    
    public void cleanup()
    {
    	cacheclient.cleanup();
    }

    public void run()
    {
    	System.out.println("Starting Thread");
    	try 
    	{
    		while(true)
    		{
				byte[] data = readBytes();
				
				if(data==null)
					continue;
				
				//System.out.println("Got Data from Connection with length: "+data.length);
				String action=null;
				String key = null;
				byte[] value;
				int actionFoundIdx = -1;
				
				//assuming key cannot have comma
				int i;
				for(i=0; i<data.length; i++)
				{
					if(actionFoundIdx==-1)
					{
						if(data[i]==',')
						{
							byte[] temp = new byte[i];
							System.arraycopy(data,0,temp,0,i);
							action = new String(temp);
							actionFoundIdx=i;
						}
					}
					else
					{
						if(action.equals("GET")||action.equals("DEL"))
							break;
						else
						{
							if(data[i]==',')
							{
								byte[] temp = new byte[i-actionFoundIdx-1];
								System.arraycopy(data,actionFoundIdx+1,temp,0,i-actionFoundIdx-1);
								key = new String(temp);
								break;
							}
						}
					}
				}
				
				if(action.equals("SET"))
				{
					value = new byte[data.length-(i+1)];
					System.arraycopy(data,i+1,value,0,data.length-(i+1));
					
					cacheSet(key.toString(),value);
				}
				else
				{
					byte[] temp = new byte[data.length-i];
					System.arraycopy(data,i,temp,0,data.length-i);
					key = new String(temp);
					
					if(action.equals("GET"))
						cacheGet(key);
					else
						cacheDelete(key);
				}
    		}
		} 
    	catch (IOException e) 
    	{
			e.printStackTrace();
		}
    }
    
    private void cacheGet(String key) throws IOException
    {
    	System.out.println("GOT: Key: "+key);
    	storeClient(key);
    	
    	byte[] result = cacheclient.get(key);
    	
    	if(result==null)
    	{
    		result = new byte[1];
    		result[0] = -1;
    	}
    	
    	sendBytes(result);
    }
    
    private void cacheSet(String key, byte[] value) throws IOException
    {  	
    	System.out.println("SET: Key: "+key+" Value: "+value);
    	storeClient(key);
    	sendBytes(new Boolean(cacheclient.set(key, value)).toString().getBytes());
    }
    
    private void cacheDelete(String key) throws IOException
    {
    	System.out.println("DEL: Key: "+key);
    	showInvalidationMap();
    	sendBytes(new Boolean(cacheclient.delete(key)).toString().getBytes());
    }
    
    private void showInvalidationMap()
    {
    	for(String key : BGCacheCoordinatorServer.invalidationMap.keySet())
    	{
    		LinkedHashSet<InetAddress> ipAddresses = BGCacheCoordinatorServer.invalidationMap.get(key);
    		
    		System.out.println("For Key "+key+" IP Addresses to be invalidated:");
    		
    		for(InetAddress ipAdd : ipAddresses)
    			System.out.print(ipAdd+",");
    		
    		System.out.println();
    	}
    }
    
    private boolean storeClient(String key)
    {
       	Object ipAddressesObj = BGCacheCoordinatorServer.invalidationMap.get(key);
    	
    	LinkedHashSet<InetAddress> ipAddresses;
    	if(ipAddressesObj!=null)
    	{
    		ipAddresses = (LinkedHashSet<InetAddress>) ipAddressesObj; 
    		ipAddresses.add(socket.getInetAddress());
    		
    	}
    	else
    	{
    		ipAddresses = new LinkedHashSet<InetAddress>();
    		ipAddresses.add(socket.getInetAddress());
    	}
    	
    	BGCacheCoordinatorServer.invalidationMap.put(key, ipAddresses);
    	return true;
    }
    
    private byte[] readBytes()
    {   
    	try
    	{
	        int len = dis.readInt();
	        byte[] data = new byte[len];
	        
	        if (len > 0) {
	            dis.readFully(data);
	        }
	        return data;
    	}
    	catch(IOException ioe)
    	{
    		return null;
    	}
    }
    
    public void sendBytes(byte[] myByteArray) throws IOException {
	    sendBytes(myByteArray, 0, myByteArray.length);
	}

	public void sendBytes(byte[] myByteArray, int start, int len) throws IOException 
	{
	    if (len < 0)
	        throw new IllegalArgumentException("Negative length not allowed");
	    if (start < 0 || start >= myByteArray.length)
	        throw new IndexOutOfBoundsException("Out of bounds: " + start);
	    
	    dos.writeInt(len);
	    if (len > 0) {
	        dos.write(myByteArray, start, len);
	    }
	}

}
