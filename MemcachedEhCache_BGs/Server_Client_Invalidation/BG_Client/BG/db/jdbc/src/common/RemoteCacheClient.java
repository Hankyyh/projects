package common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

public class RemoteCacheClient implements RemoteCacheClientConstants
{
	private Socket socket;
    Properties props;
    
    public RemoteCacheClient()
    {
    	props = new Properties();
    	props.setProperty(CACHE_SERVER_HOST, CACHE_SERVER_HOST_DEFAULT);
    	props.setProperty(CACHE_SERVER_PORT, CACHE_SERVER_PORT_DEFAULT);
    	connectToServer();
    }
    
    public void connectToServer()
	{
        try 
        {
			socket = new Socket(props.getProperty(CACHE_SERVER_HOST),new Integer(props.getProperty(CACHE_SERVER_PORT)));
		} 
        catch (NumberFormatException e) 
        {
			System.out.println("Problem with Cache Server Port No...");
			e.printStackTrace();
		} catch (UnknownHostException e) 
		{
			System.out.println("Cannot find the Remote Cache Server...");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public byte[] get(String key)
	{	
		byte[] result = null;
		
		StringBuilder identifier = new StringBuilder("GET");
		identifier.append(',');
		identifier.append(key);
		byte[] payload = identifier.toString().getBytes();
		
		try
		{
			sendBytes(payload);
			result = readBytes();
			
			if(result.length==1 && result[0]==-1)
				result=null;
			
		}
		catch(IOException ioe)
		{
			System.err.println("Not able to send payload for GET: "+ioe.getMessage());
		}
	
		return result;		
	}
    
    public boolean set(String key, byte[] value)
	{		
		StringBuilder identifier = new StringBuilder("SET");
		identifier.append(',');
		identifier.append(key);
		identifier.append(',');
		
		byte[] idBytes = identifier.toString().getBytes();
		byte[] payload  = new byte[idBytes.length+value.length];
		
		System.arraycopy(idBytes,0,payload,0,idBytes.length);
		System.arraycopy(value,0,payload,idBytes.length,value.length);
		
		String status="false";
		
		try
		{
			sendBytes(payload);
			byte[] response = readBytes();
			
			if(response != null)
			{
				status = new String(response);
			}
					
		}
		catch(IOException ioe)
		{
			System.err.println("Not able to send payload for SET: "+ioe.getMessage());
		}
			
		return new Boolean(status);
	}
    
    public boolean delete(String key)
	{
		connectToServer();
		
		StringBuilder identifier = new StringBuilder("DEL");
		identifier.append(',');
		identifier.append(key);
		
		byte[] payload = identifier.toString().getBytes();

		String status="false";
		
		try
		{
			sendBytes(payload);
			byte[] response = readBytes();
			
			if(response != null)
			{
				status = new String(response);
			}
					
		}
		catch(IOException ioe)
		{
			System.err.println("Not able to send payload for DELETE: "+ioe.getMessage());
		}
		
		return  new Boolean(status);
	}
    
    public byte[] readBytes() throws IOException 
    {
        InputStream in = socket.getInputStream();
        DataInputStream dis = new DataInputStream(in);

        int len = dis.readInt();
        byte[] data = new byte[len];
        
        if (len > 0) {
            dis.readFully(data);
        }
        return data;
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
	    
	    OutputStream out = socket.getOutputStream(); 
	    DataOutputStream dos = new DataOutputStream(out);

	    dos.writeInt(len);
	    
	    if (len > 0) {
	    	dos.write(myByteArray, start, len);
	    }
	}
}
