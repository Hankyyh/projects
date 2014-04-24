package edu.usc.bg.memcached.coordinator;

public interface RemoteMemcachedClientConstants 
{
	/**MEMCACHED CONSTANTS **/
	
	/** The name of the property for the number of fields in a record. */
	public static final String FIELD_COUNT_PROPERTY="fieldcount";

	/** Default number of fields in a record. */
	public static final String FIELD_COUNT_PROPERTY_DEFAULT="10";

	/** Representing a NULL value. */
	public static final String NULL_VALUE = "NULL";
	
	/** The field name prefix in the table.*/
	public static String COLUMN_PREFIX = "FIELD";
	
	/** The name of the property for the memcached server host name. */
	public static final String MEMCACHED_SERVER_HOST="cachehostname";
	
	/** The name of the property for the memcached server port. */
	public static final String MEMCACHED_SERVER_PORT="cacheport";
	
	/** Whether the client starts and stops the cache server. */
	public static final String MANAGE_CACHE_PROPERTY = "managecache";

	/** Whether the client starts and stops the cache server. */
	public static final String MANAGE_CACHE_PROPERTY_DEFAULT = "false";
	
	/** The name of the property for the TTL value. */
	public static final String TTL_VALUE="ttlvalue";
	
	/** The name of the property for the memcached server host name. */
	public static final String MEMCACHED_SERVER_HOST_DEFAULT="localhost";
	
	/** The name of the property for the memcached server port. */
	public static final String MEMCACHED_SERVER_PORT_DEFAULT="11211";
	
	/** The name of the property for the TTL value. */
	public static final String TTL_VALUE_DEFAULT="0";
	
	/** The name of the property for maximum acceptable staleness of data to be read (in seconds). */
	public static final String MAX_STALENESS_PROPERTY="maxstaleness";
	
	/** The name of the default value for maximum acceptable staleness of data to be read (in seconds). */
	public static final String MAX_STALENESS_PROPERTY_DEFAULT="0";
	
	/** The name of the property for controlling the shutdown of the cache connection pool during cleanup. */
	public static final String CLEANUP_CACHEPOOL_PROPERTY="cleanupcachepool";
	
	/** The name of the property for shutting down the connection pool during cleanup. */
	public static final String CLEANUP_CACHEPOOL_PROPERTY_DEFAULT="true";

}
