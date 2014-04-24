package common;

public interface RemoteCacheClientConstants 
{
	/** The name of the property for the cache server host name. */
	public static final String CACHE_SERVER_HOST="cachehostname";
	
	/** The name of the property for the cache server port. */
	public static final String CACHE_SERVER_PORT="cacheport";
	
	/** The name of the property for the cache server host name. */
	public static final String CACHE_SERVER_HOST_DEFAULT="192.168.137.97";
	
	/** The name of the property for the cache server port. */
	public static final String CACHE_SERVER_PORT_DEFAULT="7654";
	
	public static final int MAX_NUM_RETRIES   = 10;
	public static final int TIMEOUT_WAIT_MILI = 100;
}
