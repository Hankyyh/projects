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

package mongoDB;

public interface MongoDBClientConstants {
	public static final String MONGODB_URL_PROPERTY = "mongodb.url";
	public static final String MONGODB_DB_PROPERTY = "mongodb.database";
	//should be defined as strict or normal, please refer to MongoDB's manual
	public static final String MONGODB_WRITE_CONCERN_PROPERTY = "mongodb.writeConcern";
	public static final String MONGODB_WRITE_CONCERN_PROPERTY_DEFAULT = "strict";
	public static final String MONGODB_SHARDING_PROPERTY = "sharding";
	public static final String MONGODB_SHARDING_PROPERTY_DEFAULT = "false";
	public static final String MONGODB_MANIPULATION_ARRAY_PROPERTY = "manipulationarray";
	public static final String MONGODB_MANIPULATION_ARRAY_PROPERTY_DEFAULT= "false";
	public static final String MONGODB_FRNDLIST_REQ_PROPERTY = "friendlistreq";
	public static final String MONGODB_FRNDLIST_REQ_PROPERTY_DEFAULT = "false";
	//needed only for viewtopk in the client
	//which has resourse ids in the member collection
	public static final String MONGODB_SCANFORRES_PROPERTY = "scanresources";
	public static final String MONGODB_SCANFORRES_PROPERTY_DEFAULT = "true";
	public static final String MONGODB_FS_IMG_PROPERTY = "db.fspath";
	public static final String MONGODB_FS_IMG_PROPERTY_DEFAULT = "";
	
	
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
	public static final String MEMCACHED_SERVER_HOST_DEFAULT="127.0.0.1";
	
	/** The name of the property for the memcached server port. */
	public static final String MEMCACHED_SERVER_PORT_DEFAULT="11211";
	
	/** The name of the property for the TTL value. */
	public static final String TTL_VALUE_DEFAULT="0";
	
	/** The name of the property for enabling message compression. */
	public static final String ENABLE_COMPRESSION_PROPERTY="compress";
	
	/** The name of the property for the memcached server host name. */
	public static final String ENABLE_COMPRESSION_PROPERTY_DEFAULT="false";

}
