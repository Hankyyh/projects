package edu.usc.bg.memcached.coordinator;


import java.util.Date;
import java.util.Properties;
import java.util.Random;

import edu.usc.bg.memcached.coordinator.StartProcess;

import com.meetup.memcached.MemcachedClient;
import com.meetup.memcached.SockIOPool;

public class RemoteMemcachedClient implements RemoteMemcachedClientConstants
{
	private static boolean ManageCache = false;
	private static final int CACHE_START_WAIT_TIME = 10000;
	public static final int CACHE_POOL_NUM_CONNECTIONS = 400;
	private static final int MAX_NUM_RETRIES = 10;
	private static final int TIMEOUT_WAIT_MILI = 100;
	
	private static final double TTL_RANGE_PERCENT = 0.2; 
	private boolean useTTL = false;
	private int TTLvalue = 0;
	
	SockIOPool cacheConnectionPool;
	private MemcachedClient cacheclient = null;
	private static String cache_hostname = "";
	private static Integer cache_port = -1;
	StartProcess st;
	
	private Properties props = new Properties();
	
	
	private String getCacheCmd()
	{		
		return "C:\\PSTools\\psexec \\\\"+cache_hostname+" -u shahram -p 2Shahram C:\\memcached\\memcached.exe -d start ";
	}
	
	private String getCacheStopCmd()
	{
		return "C:\\PSTools\\psexec \\\\"+cache_hostname+" -u shahram -p 2Shahram C:\\memcached\\memcached.exe -d stop ";
	}
	
	
	public boolean init()
	{	
		cacheclient = new MemcachedClient();
		
		cache_hostname = props.getProperty(MEMCACHED_SERVER_HOST, MEMCACHED_SERVER_HOST_DEFAULT);
		cache_port = Integer.parseInt(props.getProperty(MEMCACHED_SERVER_PORT, MEMCACHED_SERVER_PORT_DEFAULT));
		
		ManageCache = Boolean.parseBoolean(
				props.getProperty(MANAGE_CACHE_PROPERTY, 
						MANAGE_CACHE_PROPERTY_DEFAULT));
		
		TTLvalue = Integer.parseInt(props.getProperty(TTL_VALUE, TTL_VALUE_DEFAULT));
		useTTL = (TTLvalue != 0);
		
		
		String[] servers = { cache_hostname + ":" + cache_port };
		cacheConnectionPool = SockIOPool.getInstance();
		cacheConnectionPool.setServers( servers );
		cacheConnectionPool.setFailover( true );
		cacheConnectionPool.setInitConn( 10 ); 
		cacheConnectionPool.setMinConn( 5 );
		cacheConnectionPool.setMaxConn( CACHE_POOL_NUM_CONNECTIONS );
		cacheConnectionPool.setMaintSleep( 30 );
		cacheConnectionPool.setNagle( false );
		cacheConnectionPool.setSocketTO( 3000 );
		cacheConnectionPool.setAliveCheck( true );
		cacheConnectionPool.initialize();
		
		if (ManageCache) 
		{
			System.out.println("Starting Cache: "+this.getCacheCmd());
			//this.st = new StartCOSAR(this.cache_cmd + (RaysConfig.cacheServerPort + i), "cache_output" + i + ".txt"); 
			this.st = new StartProcess(this.getCacheCmd(), "cache_output.txt");
			this.st.start();

			System.out.println("Wait for "+CACHE_START_WAIT_TIME/1000+" seconds to allow Cache to startup.");
			try{
				Thread.sleep(CACHE_START_WAIT_TIME);
			}catch(Exception e)
			{
				e.printStackTrace(System.out);
			}
		}
		
		return true;
	}
	
	public void cleanup()
	{
		cacheConnectionPool = SockIOPool.getInstance();
		cacheConnectionPool.shutDown();

		if (ManageCache){
			//MemcachedClient cache_conn = new MemcachedClient(COSARServer.cacheServerHostname, COSARServer.cacheServerPort);			
			//cache_conn.shutdownServer();
			System.out.println("Stopping Cache: "+this.getCacheStopCmd());
			//this.st = new StartCOSAR(this.cache_cmd + (RaysConfig.cacheServerPort + i), "cache_output" + i + ".txt"); 
			this.st = new StartProcess(this.getCacheStopCmd(), "cache_output.txt");
			this.st.start();
			System.out.print("Waiting for Cache to finish.");

			try
			{
				if( this.st != null )
					this.st.join();
				Thread.sleep(10000);
				System.out.println("..Done!");
			}
			catch(InterruptedException ie)
			{
				ie.printStackTrace();
			}
		}
	}
	
	public boolean set (String key, byte[] payload)
	{
		int ttl = TTLvalue;
		int numRetries = MAX_NUM_RETRIES - 1;
		boolean bool_result = false;
		
		if(useTTL)
		{
			int range = (int)(TTL_RANGE_PERCENT * TTLvalue);
			Random rand = new Random();
			if(range > 0)
			{
				ttl = rand.nextInt(2 * range) + (TTLvalue - range);
			}
		}
		
		while(numRetries < MAX_NUM_RETRIES)
		{
			try
			{
				if(useTTL)
				{					
					bool_result = cacheclient.set(key, payload, 
							new Date(
									//System.currentTimeMillis() + 
									(ttl*1000)));
				}
				else
				{
					System.out.println("SET: "+key+" & value: "+payload);
					bool_result = cacheclient.set(key, payload);
				}
				
				if(bool_result)
				{
					break;
				}
			}
			catch(RuntimeException e)
			{	
				e.printStackTrace(System.out);
				try {
					Thread.sleep(TIMEOUT_WAIT_MILI);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} 
			
			numRetries++;
		}
		
		return bool_result;
	}
	
	public byte[] get (String key)
	{
		byte[] result = null;
		int numRetries = 0;
		while(numRetries < MAX_NUM_RETRIES)
		{			
			try
			{
				//System.out.println("GET: key: "+key);
				result = (byte[]) cacheclient.get(key);		
				System.out.println("Fetched Value= "+result);
				break;
			} catch(java.nio.channels.CancelledKeyException e) {
				e.printStackTrace(System.out);
			}			
			numRetries++;
		}
		
		return result;
	}
	
	public boolean delete(String key)
	{
		boolean bool_result = false;
		int numRetries = 0;
		while(numRetries < MAX_NUM_RETRIES)
		{
			try {
				System.out.println("DEL: key: "+key);
				bool_result = cacheclient.delete(key);
				bool_result = true;
				break;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(System.out);
				//System.exit(-1);
			}
			numRetries++;
		}
		
		if (numRetries >= MAX_NUM_RETRIES) {
			return false;
		}
		return bool_result;
	}
}
