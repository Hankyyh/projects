/**                                                                                                                                                                                
 * Copyright (c) 2012 USC Database Laboratory All rights reserved. 
 *
 * Authors:  Sumita Barahmand and Shahram Ghandeharizadeh                                                                                                                            
 *                                                                                                                                                                                 
 * Licensed under the Apache License, Version 2.0 (the "License"); you                                                                                                             
 * may not use this file except in compliance with the License. You                                                                                                                
 * may obtain a copy of the License at                                                                                                                                             
 *                                                                                                                                                                                 
 * http://www.apache.org/licenses/LICENSE-2.0                                                                                                                                      
 *                                                                                                                                                                                 
 * Unless required by applicable law or agreed to in writing, software                                                                                                             
 * distributed under the License is distributed on an "AS IS" BASIS,                                                                                                               
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or                                                                                                                 
 * implied. See the License for the specific language governing                                                                                                                    
 * permissions and limitations under the License. See accompanying                                                                                                                 
 * LICENSE file.                                                                                                                                                                   
 */


package common;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.meetup.memcached.MemcachedClient;
import com.meetup.memcached.SockIOPool;

import memcached.StartProcess;


import edu.usc.bg.base.ByteIterator;
import edu.usc.bg.base.ObjectByteIterator;

public class CacheUtilities {
	public static enum ListenerCmdType {
		START, MONGO, MEMCACHED_START, MEMCACHED_STOP, MEMCACHED_START_CUSTOM
	}
	
	public static final int deserialize_buffer_size 	= 1024*1024 * 5;
	
	public static final int MAX_UPDATE_RETRIES 			= 100;
	public static final int UPDATE_RETRY_SLEEP_TIME 	= 10; // milliseconds that gets added after every failure
	
	private static final int MAX_NUM_RETRIES 			= 10;
	private static final int TIMEOUT_WAIT_MILI 			= 100;
 
	public static final boolean LOCK_TABLE_EXPLICIT 	= false;	// Manually lock tables to disable effect of MVCC not working with Gumball
	
	public static boolean USE_LISTENER_START_CACHE 		= true;
	private static final int COSAR_WAIT_TIME 			= 10000;
	
	private static final double TTL_RANGE_PERCENT 		= 0.2; 
	public static final int CACHE_POOL_NUM_CONNECTIONS = 400;
	
	public static byte[] compressBytes(byte[] input) throws UnsupportedEncodingException, IOException
    {
        //byte[] input = data.getBytes("UTF-8");  //the format... data is the total string
        Deflater df = new Deflater();       //this function mainly generate the byte code
        df.setLevel(Deflater.BEST_COMPRESSION);
        df.setInput(input);
 
        ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length);   //we write the generated byte code in this array
        df.finish();
        byte[] buff = new byte[1024];   //segment segment pop....segment set 1024
        while(!df.finished())
        {
            int count = df.deflate(buff);       //returns the generated code... index
            baos.write(buff, 0, count);     //write 4m 0 to count
        }
        baos.close();
        byte[] output = baos.toByteArray();
        
        return output;
    }
 
	public static byte[] decompressBytes(byte[] input) throws UnsupportedEncodingException, IOException, DataFormatException
    {
        Inflater ifl = new Inflater();   //mainly generate the extraction
        ifl.setInput(input);
 
        ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length);
        byte[] buff = new byte[1024];
        while(!ifl.finished())
        {
            int count = ifl.inflate(buff);
            baos.write(buff, 0, count);
        }
        baos.close();
        byte[] output = baos.toByteArray();
        
        return output;
    }

	public static byte[] SerializeHashMap(HashMap<String, ByteIterator> m){
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bos);
		try {
			out.writeInt(m.size());


			for (String s: m.keySet()){
				out.writeInt(s.length());
				out.writeBytes(s);
				ByteIterator vBI = m.get(s);

				//String v = vBI.toString();
				byte[] v = vBI.toArray();
				out.writeInt(v.length);
				out.write(v);
				
				/* Offset has to be reset because toArray consumes the data.
				 * Needed because the StringByteIterator toString works does not
				 *  follow the description in ByteIterator (returns a reference
				 *  to the original string
				 *  without consuming the data if called before anyone
				 *  has started consuming data). Because of this, outside code
				 *  has been using toString to repeatedly obtain data when
				 *  they shouldn't be using ByteIterator like that.
				 */				
				if( vBI instanceof ObjectByteIterator )
				{
					((ObjectByteIterator)vBI).resetOffset();
				}
				
			}
		} catch (Exception e) {
			System.out.println("Error, ApplicationCacheClient failed to serialize HashMap.  This is a catastrophic error");
			e.printStackTrace(System.out);
		} 
		finally {
			try {
				out.flush();
				out.close();
			} catch (Exception e) {
				System.out.println("Error, ApplicationCacheClient failed to flush output buffers.");
				e.printStackTrace(System.out);
			}
		}		
		return bos.toByteArray();
	}

	public static byte[] SerializeVectorOfHashMaps(Vector<HashMap<String, ByteIterator>> m){
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bos);
		try {
			out.writeInt(m.size());
			for (int i=0; i < m.size(); i++){
				byte[] oneHM = SerializeHashMap(m.elementAt(i));
				out.write(oneHM, 0, oneHM.length);
			}
		} catch (Exception e) {
			System.out.println("Error, ApplicationCacheClient failed to serialize HashMap.  This is a catastrophic error");
			e.printStackTrace(System.out);
		} 
		finally {
			try {
				out.flush();
				out.close();
			} catch (Exception e) {
				System.out.println("Error, ApplicationCacheClient failed to flush output buffers.");
				e.printStackTrace(System.out);
			}
		}		
		return bos.toByteArray();
	}
	
	public static byte[] SerializeVectorOfInts(Vector<Integer> m){
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bos);
		try {
			out.writeInt(m.size());
			for (int i=0; i < m.size(); i++){
				out.writeInt(m.get(i));
			}
		} catch (Exception e) {
			System.out.println("Error in SerializeVectorOfInts, ApplicationCacheClient failed to serialize Vector of Integer.  This is a catastrophic error");
			e.printStackTrace(System.out);
		} 
		finally {
			try {
				out.flush();
				out.close();
			} catch (Exception e) {
				System.out.println("Error in SerializeVectorOfInts, ApplicationCacheClient failed to flush output buffers.");
				e.printStackTrace(System.out);
			}
		}		
		return bos.toByteArray();
	}

	private static void readObject( DataInputStream in, int num_bytes, byte[] byte_array ) throws IOException
	{
		int total_bytes_read = 0;
		int bytes_read = 0;

		while( total_bytes_read < num_bytes )
		{
			bytes_read = in.read(byte_array, total_bytes_read, num_bytes - total_bytes_read);
			total_bytes_read += bytes_read;
		}		
	}
	
	public static boolean unMarshallHashMap(HashMap<String, ByteIterator> m, byte[] payload){
		return unMarshallHashMap(m, payload, new byte[deserialize_buffer_size]);
	}

	public static boolean unMarshallHashMap(HashMap<String, ByteIterator> m, byte[] payload, byte[] read_buffer){
		boolean result = true;
		try {
			// Read from byte_array
			ByteArrayInputStream bis = new ByteArrayInputStream(payload);
			DataInputStream in = new DataInputStream(bis);
			int numkeys = in.readInt();

			byte[] buffer = read_buffer;//new byte[deserialize_buffer_size];
			int field_size = 0;		
			String key = null;
			byte[] val = null;
			//String val;
			for( int k = 0; k < numkeys; k++ )
			{
				field_size = in.readInt();
				readObject(in, field_size, buffer);
				key = new String(buffer, 0, field_size);

				field_size = in.readInt();
				readObject(in, field_size, buffer);
				//val = new String(buffer, 0, field_size);
				val = new byte[field_size];
				System.arraycopy( buffer, 0, val, 0, field_size );

				//m.put(key, new StringByteIterator(val, 0, field_size)) ;
				m.put(key, new ObjectByteIterator(val));
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
			System.out.println("Error, ApplicationCacheClient failed to unMarshall bytearray into a HashMap.");
		}
		return result;
	}
	
	
	public static boolean unMarshallVectorOfHashMaps(byte[] payload, Vector<HashMap<String, ByteIterator>> V){
		return unMarshallVectorOfHashMaps(payload, V, new byte[deserialize_buffer_size]);
	}

	public static boolean unMarshallVectorOfHashMaps(byte[] payload, Vector<HashMap<String, ByteIterator>> V, byte[] read_buffer){
		boolean result = true;
		byte[] buffer = read_buffer;//new byte[deserialize_buffer_size];
		//Vector<HashMap<String, ByteIterator>> V = new Vector<HashMap<String, ByteIterator>> ();
		try {
			// Read from byte_array
			ByteArrayInputStream bis = new ByteArrayInputStream(payload);
			DataInputStream in = new DataInputStream(bis);
			int numelts = in.readInt();
			for (int i=0; i < numelts; i++){
				//buffer = new byte[deserialize_buffer_size];
				int field_size = 0;		
				String key = null;
				byte[] val = null;
				//String val;
				int numkeys = in.readInt();
				HashMap<String, ByteIterator> m = new HashMap<String, ByteIterator>();
				for( int k = 0; k < numkeys; k++ )
				{
					field_size = in.readInt();
					readObject(in, field_size, buffer);
					key = new String(buffer, 0, field_size);

					field_size = in.readInt();
					readObject(in, field_size, buffer);
					//val = new String(buffer, 0, field_size);
					val = new byte[field_size];
					System.arraycopy( buffer, 0, val, 0, field_size );

					//m.put(key, new ObjectByteIterator(val.getBytes())) ;
					m.put(key, new ObjectByteIterator(val)) ;
				}
				V.addElement(m);
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
			System.out.println("Error, ApplicationCacheClient failed to unMarshall bytearray into a HashMap.");
			result = false;
		}	
		return result;
	}
	
	public static boolean unMarshallVectorOfInts(byte[] payload, Vector<Integer> V){
		boolean result = true;
		int value;
		try {
			// Read from byte_array
			ByteArrayInputStream bis = new ByteArrayInputStream(payload);
			DataInputStream in = new DataInputStream(bis);
			int numelts = in.readInt();
			for (int i=0; i < numelts; i++){
				value = in.readInt();
				V.addElement(value);
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
			System.out.println("Error, ApplicationCacheClient failed to unMarshall bytearray into a Vector of Integer.");
			result = false;
		}	
		return result;
	}
	
	
	public static String getLockStatement(String tablename, boolean exclusiveMode )
	{			
		if(exclusiveMode)
		{
			return "LOCK TABLE " + tablename + " IN EXCLUSIVE MODE";
		}
		else
		{
			return "LOCK TABLE " + tablename + " IN ROW SHARE MODE";
		}
	}
	
	public static void LockRow(PreparedStatement pstmt, String tablename, boolean exclusiveMode) throws SQLException
	{
		pstmt.executeUpdate(getLockStatement(tablename, exclusiveMode));
	}
	
	
	public static ResultSet ExecuteQuery(Connection conn, 
			PreparedStatement preparedStatement, 
			PreparedStatement lockStatement, 
			String tablename) 
			throws SQLException
	{
		String tablename_array[] = {tablename};
		return ExecuteQuery(conn, preparedStatement, lockStatement, tablename_array);
	}
			
	public static ResultSet ExecuteQuery(Connection conn, 
			PreparedStatement preparedStatement, 
			PreparedStatement lockStatement, 
			String[] tablenames) 
			throws SQLException
	{
		boolean prev_autocommit_val = conn.getAutoCommit();
		ResultSet rs = null;
		
		conn.setAutoCommit(false);
		try
		{
			if(LOCK_TABLE_EXPLICIT)
			{
				for(String tablename : tablenames)
				{
					LockRow(lockStatement, tablename, false);
				}
			}
			rs = preparedStatement.executeQuery();
			conn.commit();
		}
		catch(SQLException e)
		{
			conn.rollback();
			conn.setAutoCommit(prev_autocommit_val);
			throw e;
		}
		conn.setAutoCommit(prev_autocommit_val);
		return rs;
	}
	
	public static boolean ExecuteUpdateStmt(Connection conn, 
			PreparedStatement pstmt,
			PreparedStatement lockStatement,
			String tablename) throws SQLException {
		String name_array[] = {tablename};
		return ExecuteUpdateStmt(conn, pstmt, lockStatement, name_array);
	}
	
	public static boolean ExecuteUpdateStmt(Connection conn, 
			PreparedStatement pstmt,
			PreparedStatement lockStatement,
			String tablenames[]) throws SQLException
	{
		boolean retVal = false;
		int num_retries = 0;
		int sleep_time = 1;
		Random rand = new Random();
		
		boolean prev_autocommit_val = conn.getAutoCommit();
		conn.setAutoCommit(false);
		while(retVal == false)
		{
			try {
				if(LOCK_TABLE_EXPLICIT)
				{
					for(String tablename : tablenames)
					{
						LockRow(lockStatement, tablename, true);
					}
				}
				pstmt.executeUpdate();
				conn.commit();
				retVal = true;
				//System.out.println("Successfully updated");
			} catch (SQLException e) {
				conn.rollback();
				if(e.getMessage().indexOf("ORA-08177") >= 0 && num_retries < MAX_UPDATE_RETRIES)
				{					
					// Sleep for awhile to avoid constantly deadlocking with other transactions
					try {
						int temp = rand.nextInt(sleep_time);
						Thread.sleep(temp);
						//System.out.println("Sleep for: " + temp + " ms, " + num_retries + " time");
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					// Retry the execution
					num_retries++;
					sleep_time += UPDATE_RETRY_SLEEP_TIME;					
				}
				else
				{
					conn.setAutoCommit(prev_autocommit_val);
					throw e;
				}
			}
		}
		conn.setAutoCommit(prev_autocommit_val);
		return retVal;
	}
		
	private static String getCacheCmd(String cache_hostname)
	{	
		return "C:\\PSTools\\psexec \\\\"+cache_hostname+
					" -u shahram -p 2Shahram " +
					"C:\\cosar\\configurable\\TCache.NetworkInterface.exe C:\\cosar\\configurable\\V2gb.xml ";
	}
	
	public static void runListener(String listener_hostname, int listener_port, String command)
	{
		runListener(listener_hostname, listener_port, ListenerCmdType.START, command);
	}
	
	public static void startMemcached(String listener_hostname, int listener_port)
	{
		runListener(listener_hostname, listener_port, ListenerCmdType.MEMCACHED_START, "");
	}
	
	public static void stopMemcached(String listener_hostname, int listener_port)
	{
		runListener(listener_hostname, listener_port, ListenerCmdType.MEMCACHED_STOP, "");
	}
		
	public static void runListener(String listener_hostname, int listener_port, ListenerCmdType type, String command)
	{
		DataInputStream in = null;
		DataOutputStream out = null;
		
		try {
			
			Socket conn = new Socket(listener_hostname, listener_port);
			in = new DataInputStream(conn.getInputStream());
			out = new DataOutputStream(conn.getOutputStream());
			
			switch(type)
			{
			case START:
				out.writeBytes("start ");
				break;
			case MONGO:
				out.writeBytes("mongo ");				
				break;
			case MEMCACHED_START:
				out.writeBytes("memch ");
				command = "start";
				break;
			case MEMCACHED_START_CUSTOM:
				out.writeBytes("memch ");
				command = "startcustom " + command;
				break;
			case MEMCACHED_STOP:
				out.writeBytes("memch ");
				command = "stop";
				break;				
			default:
				break;
			}
			
			out.writeInt(command.length());
			out.writeBytes(command);
			out.flush();
			
			
			int response = in.readInt();
			if(response != 0)
			{
				System.out.println("Error starting process");
			}
			
			out.close();
			in.close();
			conn.close();
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void startCOSAR(String cache_hostname, int listener_port)
	{
		if(USE_LISTENER_START_CACHE)
		{
			String command = "C:\\cosar\\configurable\\TCache.NetworkInterface.exe C:\\cosar\\configurable\\V2gb.xml ";
			runListener(cache_hostname, listener_port, command);
		}
		else
		{
			System.out.println("Starting COSAR: "+getCacheCmd(cache_hostname));
			//this.st = new StartCOSAR(this.cache_cmd + (RaysConfig.cacheServerPort + i), "cache_output" + i + ".txt"); 
			StartProcess st = new StartProcess(getCacheCmd(cache_hostname), "cache_output.txt");
			st.start();
		}

		System.out.println("Wait for "+COSAR_WAIT_TIME/1000+" seconds to allow COSAR to startup.");
		try{
			Thread.sleep(COSAR_WAIT_TIME);
		}catch(Exception e)
		{
			e.printStackTrace(System.out);
		}
	}	
	
	public static String getMyIPAddress()
	{
		String memCachedIP = memcachedEhcache.MemcachedEhcacheClientConstants.INVALIDATION_SERVER_HOST_DEFAULT;
        String[] ipAddressClass = memCachedIP.split("\\.");
        
        String ipAddressPrefix = ipAddressClass[0]+"."+ipAddressClass[1]+"."+ipAddressClass[2];
		
		try
		{
			Enumeration<NetworkInterface> e=NetworkInterface.getNetworkInterfaces();
	        while(e.hasMoreElements())
	        {
	            NetworkInterface n=(NetworkInterface) e.nextElement();
	            Enumeration<InetAddress> ee = n.getInetAddresses();
	            while(ee.hasMoreElements())
	            {
	                InetAddress i= (InetAddress) ee.nextElement();
	                
	                if(i.getHostAddress().toString().startsWith(ipAddressPrefix))
	                	return i.getHostAddress().toString();
	            }
	        }
	        return null;
		}
		catch(SocketException se)
		{
			System.out.println("Error fetching my IP Address...");
			return null;
		}
	}
	
	public static byte[] CacheEMGet(MemcachedClient cacheclient, String key, boolean compressPayload)
	{
		String invalidationKey = key+"_inv";
		//cacheclient.get(invalidationKey);
		
		SockIOPool cacheConnectionPool;
		Properties props = memcachedEhcache.MemcachedEhcacheClient.props;

		String cache_hostname = props.getProperty(memcachedEhcache.MemcachedEhcacheClientConstants.INVALIDATION_SERVER_HOST_DEFAULT, memcachedEhcache.MemcachedEhcacheClientConstants.INVALIDATION_SERVER_HOST_DEFAULT);
		Integer cache_port = Integer.parseInt(props.getProperty(memcachedEhcache.MemcachedEhcacheClientConstants.INVALIDATION_SERVER_PORT_DEFAULT, memcachedEhcache.MemcachedEhcacheClientConstants.INVALIDATION_SERVER_PORT_DEFAULT));		
		
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

		HashSet<String> ipAddresses =  (HashSet<String>) cacheclient.get(invalidationKey);

		String myIPAddress = CacheClientConstants.IP_ADDRESS;
		//String myIPAddress = getMyIPAddress();
		
		if(myIPAddress!=null)
		{
			if(ipAddresses!=null)
			{
				if(!ipAddresses.contains(myIPAddress))
				{
					System.out.println("Setting "+myIPAddress+" to already set IPAddresses of :"+ipAddresses+" for key: "+invalidationKey);
					ipAddresses.add(myIPAddress);
					cacheclient.set(invalidationKey,ipAddresses);
				}
			}
			else
			{
				System.out.println("Setting "+myIPAddress+" as the 1st IPAddress for key: "+invalidationKey);
				ipAddresses = new HashSet<String>();
				ipAddresses.add(myIPAddress);
				cacheclient.set(invalidationKey,ipAddresses);
			}
		}

		return CacheGet(cacheclient,key,compressPayload);
	}
	
	public static byte[] CacheGet(MemcachedClient cacheclient, String key, boolean compressPayload)
	{		
		byte[] result = null;
		int numRetries = 0;
		while(numRetries < MAX_NUM_RETRIES)
		{			
			try
			{
				result = (byte[]) cacheclient.get(key);				
				break;
			} catch(java.nio.channels.CancelledKeyException e) {
				e.printStackTrace(System.out);
			}			
			numRetries++;
		}
		
		if(result != null && compressPayload)
		{
			try {
				result = CacheUtilities.decompressBytes(result);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				result = null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				result = null;
			} catch (DataFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				result = null;
			}
		}
		
		return result;
	}
		
	
	public static boolean CacheEMSet(MemcachedClient cacheclient, String key, byte[] payload, boolean useTTL, int TTLvalue, boolean compressPayload)
	{	
		String invalidationKey = key+"_inv";
		HashSet<String> ipAddresses =  (HashSet<String>) cacheclient.get(invalidationKey);
		
		//String myIPAddress = getMyIPAddress();
		String myIPAddress = CacheClientConstants.IP_ADDRESS;;
		
		if(myIPAddress!=null)
		{
			if(ipAddresses!=null)
			{
				if(!ipAddresses.contains(myIPAddress))
				{
					System.out.println("Setting "+myIPAddress+" to already set IPAddresses of :"+ipAddresses+" for key: "+invalidationKey);
					ipAddresses.add(myIPAddress);
					cacheclient.set(invalidationKey,ipAddresses);
				}
			}
			else
			{
				System.out.println("Setting "+myIPAddress+" as the 1st IPAddress for key: "+invalidationKey);
				ipAddresses = new HashSet<String>();
				ipAddresses.add(myIPAddress);
				cacheclient.set(invalidationKey,ipAddresses);
			}
		}
		
		return CacheSet(cacheclient,key,payload,useTTL,TTLvalue,compressPayload);
	}
	
	public static boolean CacheSet(MemcachedClient cacheclient, String key, byte[] payload, boolean useTTL, int TTLvalue, boolean compressPayload)
	{		
		boolean bool_result = false;
		int numRetries = MAX_NUM_RETRIES - 1;
		int ttl = TTLvalue;
		
		if(payload != null && compressPayload)
		{
			try {
				payload = CacheUtilities.compressBytes(payload);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(System.out);
				return false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(System.out);
				return false;
			}
		}
		
		// Use random TTL so that all entries don't expire simultaneously
		// Random value should be within range of the specified TTL value
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
	
	public static boolean CacheEMDelete(MemcachedClient cacheclient, String key)
	{
		String invalidationKey = key+"_inv";
		HashSet<String> ipAddresses =  (HashSet<String>) cacheclient.get(invalidationKey);
		Socket socket=null;
		
		if(ipAddresses!=null)
		{
			for(String ipAddress : ipAddresses)
			{
				System.out.println("INVALIDATING ALL EHCACHES OF THE IPADDRESS: "+ipAddress+" for key: "+key);
				
				try 
		        {
					socket = new Socket(ipAddress,new Integer(memcachedEhcache.MemcachedEhcacheClientConstants.LOCAL_MEMCACHEDEHCACHE_SERVER_PORT_DEFAULT));
					sendBytes(socket, key.getBytes());
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
			
			System.out.println("DELETING RECORD FROM INVALIDATING MEMCACHE for key: "+invalidationKey);
			cacheclient.delete(invalidationKey);
		}
		
		return CacheDelete(cacheclient, key);
	}
	
	public static boolean CacheDelete(MemcachedClient cacheclient, String key)
	{
		boolean bool_result = false;
		int numRetries = 0;
		while(numRetries < MAX_NUM_RETRIES)
		{
			try {
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
	
	 public static void sendBytes(Socket socket, byte[] myByteArray) throws IOException {
		    sendBytes(socket, myByteArray, 0, myByteArray.length);
		}

	public static void sendBytes(Socket socket, byte[] myByteArray, int start, int len) throws IOException 
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
