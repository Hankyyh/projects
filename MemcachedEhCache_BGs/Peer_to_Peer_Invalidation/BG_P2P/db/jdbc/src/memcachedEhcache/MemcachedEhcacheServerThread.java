package memcachedEhcache;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import net.sf.ehcache.Cache;

public class MemcachedEhcacheServerThread extends Thread
{
	private Socket socket = null;

    public MemcachedEhcacheServerThread(Socket socket) 
    {
        super("MemcachedEhcacheServerThread");
        this.socket = socket;
    }

    public void run()
    {
    	try 
    	{
			byte[] payload = readBytes();
			String key = new String(payload);
			
			System.out.println("Invalidating Key "+key+" from EhCache...");
			MemcachedEhcacheClient.CM.remove(key);
		} 
    	catch (IOException e) 
    	{
			e.printStackTrace();
		}
    }
    
    private byte[] readBytes() throws IOException 
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
}
