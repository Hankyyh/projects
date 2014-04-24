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

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.CommandResult;
import com.mongodb.DBAddress;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoOptions;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.mongodb.gridfs.*;

import edu.usc.bg.base.ByteIterator;
import edu.usc.bg.base.Client;
import edu.usc.bg.base.DB;
import edu.usc.bg.base.DBException;
import edu.usc.bg.base.ObjectByteIterator;

/**
 * MongoDB client for BG framework.
 * 
 * Properties to set:
 * 
 * mongodb.url=mongodb://localhost:27017 mongodb.database=benchmark
 * mongodb.writeConcern=normal
 * 
 * 
 * 
 */
public class MongoDbClientAllImagesAsByteArrays extends DB implements
MongoDBClientConstants {

	private static Mongo mongo;
	private WriteConcern writeConcern;
	private String database;
	private boolean manipulationArray;
	private boolean friendListReq;
	private boolean scanResources;
	private static AtomicInteger NumThreads = null;
	private static Semaphore crtcl = new Semaphore(1, true);
	//private static Vector<Mongo> mongoConns = new Vector<Mongo>();
	private static Properties props;

	private static int incrementNumThreads() {
		int v;
		do {
			v = NumThreads.get();
		} while (!NumThreads.compareAndSet(v, v + 1));
		return v + 1;
	}

	private static int decrementNumThreads() {
		int v;
		do {
			v = NumThreads.get();
		} while (!NumThreads.compareAndSet(v, v - 1));
		return v - 1;
	}

	/**
	 * Initialize any state for this DB. Called once per DB instance; there is
	 * one DB instance per client thread.
	 */
	public boolean init() throws DBException {
		// initialize MongoDb driver
		props = getProperties();
		String url = props.getProperty(MONGODB_URL_PROPERTY);
		database = props.getProperty(MONGODB_DB_PROPERTY);
		String writeConcernType = props
				.getProperty(MONGODB_WRITE_CONCERN_PROPERTY, MONGODB_WRITE_CONCERN_PROPERTY_DEFAULT);
		manipulationArray = Boolean.parseBoolean(props.getProperty(
				MONGODB_MANIPULATION_ARRAY_PROPERTY,
				MONGODB_MANIPULATION_ARRAY_PROPERTY_DEFAULT));
		friendListReq = Boolean.parseBoolean(props.getProperty(
				MONGODB_FRNDLIST_REQ_PROPERTY,
				MONGODB_FRNDLIST_REQ_PROPERTY_DEFAULT));
		scanResources = Boolean.parseBoolean(props.getProperty(
				MONGODB_SCANFORRES_PROPERTY,
				MONGODB_SCANFORRES_PROPERTY_DEFAULT));

		if ("none".equals(writeConcernType)) {
			// don't return error on writes
			writeConcern = WriteConcern.NONE;
		} else if ("strict".equals(writeConcernType)) {
			// The write will wait for a response from the server and raise an
			// exception on any error
			writeConcern = WriteConcern.SAFE;
		} else if ("normal".equals(writeConcernType)) {
			// normal error handling - just raise exceptions when problems, don
			// wait for response form servers
			writeConcern = WriteConcern.NORMAL;
		}

		try {
			// System.out.println("new database url = "+url);
			/*
			 * MongoOptions mo = new MongoOptions(); mo.connectionsPerHost =
			 * 100; mongo = new Mongo(new DBAddress(url), mo);
			 */
			
			/*
			 * List<ServerAddress> addrs = new ArrayList<ServerAddress>();
			 * addrs.add( new ServerAddress( "10.0.0.122" , 27017 ) );
			 * addrs.add( new ServerAddress( "10.0.0.122" , 10002 ) );
			 * addrs.add( new ServerAddress( "10.0.0.120" , 10003 ) );
			 * MongoOptions mongoOptions = new MongoOptions(); mongo = new
			 * Mongo( addrs, mongoOptions);
			 * mongo.setReadPreference(ReadPreference.SECONDARY);
			 */
			// System.out.println("mongo connection created with "+url);
			try {
				crtcl.acquire();

				if (NumThreads == null) {
					NumThreads = new AtomicInteger();
					NumThreads.set(0);
					MongoOptions mo = new MongoOptions();
					mo.connectionsPerHost = 100;
					String urls[] ;
					if(!url.contains(";")){ //multiple mongos servers
						url += "/" + database;
						mongo = new Mongo(new DBAddress(url), mo);
					}/*else{ // need to append db to url.
						urls = url.split(";");
						 List<ServerAddress> addrs = new ArrayList<ServerAddress>();
						 for(int i=0; i< urls.length; i++){
							 addrs.add( new ServerAddress(urls[i].split(":")[0] , Integer.parseInt(urls[i].split(":")[1]) ) );
							 //no need to add the database name here as each action does a mongo.getDB(database)
						 } 
						 mongo = new Mongo( addrs);
					}*/
					else{ // need to append db to url.
						urls = url.split(";");
						mo = new MongoOptions();
						mo.connectionsPerHost = 100;
						//trying to direct clients to different routers
						url = urls[(Integer.parseInt(props.getProperty(Client.MACHINE_ID_PROPERTY, "0")))%urls.length];
						url += "/" + database;
						 mongo = new Mongo(new DBAddress(url), mo);
					}
					 
					
					
					//mongo = new MongoClient(new DBAddress(url));
					// checking to see if the connection is established
					try {
						Socket socket = mongo.getMongoOptions().socketFactory
								.createSocket();
						socket.connect(mongo.getAddress().getSocketAddress());
						socket.close();
					} catch (IOException ex) {
						System.out
						.println("ERROR: Can't create connection, check if MongDB is running");
						return false;
					}
				}
				incrementNumThreads();

			} catch (Exception e) {
				System.out.println("MongoDB init failed to acquire semaphore.");
				e.printStackTrace(System.out);
			} finally {
				crtcl.release();
			}
		} catch (Exception e1) {
			System.out
			.println("Could not initialize MongoDB connection pool for Loader: "
					+ e1.toString());
			e1.printStackTrace(System.out);
			return false;
		}
		return true;
	}

	@Override
	/**
	 * Insert a record in the database. Any field/value pairs in the specified values HashMap will be written into the record with the specified
	 * record key.
	 *
	 * @param entitySet The name of the table
	 * @param entityPK The record key of the record to insert.
	 * @param values A HashMap of field/value pairs to insert in the record
	 * @return Zero on success, a non-zero error code on error. See this class's description for a discussion of error codes.
	 */
	public int insertEntity(String entitySet, String entityPK,
			HashMap<String, ByteIterator> values, boolean insertImage) {
		com.mongodb.DB db = null;
		WriteResult res = null;
		try {
			// get the appropriate database
			db = mongo.getDB(database);
			// ensure order
			db.requestStart();
			// appropriate table - collection
			DBCollection collection = db.getCollection(entitySet);
			// create the row-object-document
			// need to insert key as integer else the sorting based on id for
			// topk s wont happen properly

			DBObject r = new BasicDBObject().append("_id",
					Integer.parseInt(entityPK));
			for (String k : values.keySet()) {
				if (!(k.toString().equalsIgnoreCase("pic") || k.toString()
						.equalsIgnoreCase("tpic")))
					r.put(k, values.get(k).toString());
			}

			if (entitySet.equalsIgnoreCase("users")) {
				// ArrayList x = new ArrayList();
				r.put("ConfFriends", new ArrayList<Integer>());
				r.put("PendFriends", new ArrayList<Integer>());
				r.put("wallResources", new ArrayList<Integer>());
			}

			if (entitySet.equalsIgnoreCase("users") && insertImage) {
				// insert picture separately
				// create one gridFS for the datastore
				byte[] profileImage = ((ObjectByteIterator) values.get("pic"))
						.toArray();
				r.put("pic", profileImage);
				// create the thumbnail image
				/*
				 * File file = new File("mypic.jpg"); byte fileContent[] = null;
				 * try{ //create FileInputStream object FileInputStream fin =
				 * new FileInputStream(file); fileContent = new
				 * byte[(int)file.length()]; fin.read(fileContent);
				 * }catch(Exception e){ e.printStackTrace(); } byte[] thumbImage
				 * = fileContent;
				 */

				byte[] thumbImage = ((ObjectByteIterator) values.get("tpic"))
						.toArray();
				r.put("tpic", thumbImage);

			}

			res = collection.insert(r, writeConcern);

			if (entitySet.equalsIgnoreCase("resources")) {
				// add the rid to the walluserids profile
				collection = db.getCollection("users");
				DBObject q = new BasicDBObject().append("_id",
						Integer.parseInt((String) r.get("walluserid")));
				DBObject queryResult = null;
				queryResult = collection.findOne(q);
				String x = queryResult.get("wallResources").toString();
				BasicDBObject updateCommand = new BasicDBObject();
				if (!x.equals("[ ]")) {
					x = x.substring(1, x.length() - 1);
					String resourceIds[] = x.split(",");
					int[] resIds = new int[resourceIds.length + 1];
					for (int i = 0; i < resourceIds.length; i++) { // to limit
						// it
						resIds[i] = Integer.parseInt(resourceIds[i].trim());
					}
					resIds[resourceIds.length] = Integer.parseInt(r.get("_id")
							.toString());
					Arrays.sort(resIds);
					updateCommand.put("$set", new BasicDBObject(
							"wallResources", resIds));
				} else
					updateCommand.put("$push", new BasicDBObject(
							"wallResources", r.get("_id")));
				res = collection.update(q, updateCommand, false, false,
						writeConcern);
			}

			/*
			 * // test to see if inserted - search query BasicDBObject
			 * searchQuery = new BasicDBObject(); searchQuery.put("_id",
			 * Integer.parseInt(key)); // query it DBCursor cursor =
			 * collection.find(searchQuery); // loop over the cursor and display
			 * the retrieved result while (cursor.hasNext()) {
			 * System.out.println(cursor.next()); } return 0;
			 */
		} catch (Exception e) {
			System.out.println(e.toString());
			return -1;
		} finally {
			if (db != null) {
				db.requestDone();
			}
		}
		return res.getError() == null ? 0 : -1;
	}

	@Override
	public int viewProfile(int requesterID, int profileOwnerID,
			HashMap<String, ByteIterator> result, boolean insertImage,
			boolean testMode) {

		int retVal = 0;
		if (requesterID < 0 || profileOwnerID < 0)
			return -1;

		com.mongodb.DB db = null;
		try {
			db = mongo.getDB(database);
			db.requestStart();
			DBCollection collection = db.getCollection("users");
			DBObject q = new BasicDBObject().append("_id", profileOwnerID);
			q.removeField("tpic");
			BasicDBObject attrs = new BasicDBObject("tpic", 0);

			DBObject queryResult = null;
			queryResult = collection.findOne(q, attrs);

			if (queryResult.get("pic") != null) {
				byte[] myPic = (byte[]) queryResult.get("pic");
				result.put("pic", new ObjectByteIterator(myPic));
				if (testMode) {
					// Save loaded image from database into new image file
					FileOutputStream fos = new FileOutputStream(profileOwnerID
							+ "--proimage.bmp");
					fos.write(myPic);
					fos.close();
				}
				queryResult.removeField("pic");
			}

			String x = queryResult.get("ConfFriends").toString();
			int frndCount = 0;
			if (x.equals("")
					|| (!x.equals("") && (x.substring(2, x.length() - 1))
							.equals("")))
				frndCount = 0;
			else {
				x = x.substring(2, x.length() - 1);
				frndCount = x.split(",").length;
			}

			int pendCount = 0;
			if (requesterID == profileOwnerID) {
				x = queryResult.get("PendFriends").toString();
				if (x.equals("")
						|| (!x.equals("") && (x.substring(2, x.length() - 1))
								.equals("")))
					pendCount = 0;
				else {
					x = x.substring(2, x.length() - 1);
					pendCount = x.split(",").length;
				}
			}

			// find number of resources posted for the user
			// queryResult = null;
			// queryResult = collection.findOne(q);
			x = queryResult.get("wallResources").toString();
			int resCount = 0;
			if (x.equals(""))
				resCount = 0;
			else {
				resCount = x.split(",").length;
			}

			if (queryResult != null) {
				// remove the ConfFriends and PendFriends and Resources
				// replace them with counts
				queryResult.removeField("ConfFriends");
				queryResult.removeField("PendFriends");
				queryResult.removeField("wallResources");
				result.putAll(queryResult.toMap());
				result.put("friendcount", new ObjectByteIterator(Integer
						.toString(frndCount).getBytes()));
				if (requesterID == profileOwnerID) {
					result.put("pendingcount", new ObjectByteIterator(Integer
							.toString(pendCount).getBytes()));
				}
				result.put("resourcecount", new ObjectByteIterator(Integer
						.toString(resCount).getBytes()));
			}

		} catch (Exception e) {
			System.out.println(e.toString());
			retVal = -1;
		} finally {
			if (db != null) {
				db.requestDone();
			}
		}
		return retVal;
	}

	@Override
	public int listFriends(int requesterID, int profileOwnerID,
			Set<String> fields, Vector<HashMap<String, ByteIterator>> result,
			boolean insertImage, boolean testMode) {
		int retVal = 0;
		if (requesterID < 0 || profileOwnerID < 0)
			return -1;

		// first get all confirmed friendids for profileOwnerID
		com.mongodb.DB db = null;
		try {
			db = mongo.getDB(database);
			db.requestStart();
			DBCollection collection = db.getCollection("users");
			DBObject q = new BasicDBObject().append("_id", profileOwnerID);
			q.removeField("pic");
			BasicDBObject attrs = new BasicDBObject("pic", 0);

			DBObject queryResult = null;
			queryResult = collection.findOne(q, attrs);

			String x = queryResult.get("ConfFriends").toString();

			if (!x.equals("")) {
				x = x.substring(2, x.length() - 1);
				if (!x.equals("")) {
					String friendIds[] = x.split(",");
					BasicDBObject query = new BasicDBObject();
					if (!friendListReq) {
						List<Integer> list = new ArrayList<Integer>();
						for (int i = 0; i < friendIds.length; i++) {
							// add to list
							list.add(Integer.parseInt(friendIds[i].trim()));
							int cnt = 0;
							if (i == friendIds.length - 1
									|| ((i + 1) % 10) == 0) {
								// query
								query.put("_id", new BasicDBObject("$in", list));
								query.removeField("pic");
								// DBCursor cursor = collection.find(query,
								// fieldsObj);
								DBCursor cursor = collection.find(query, attrs);
								while (cursor.hasNext()) {
									cnt++;
									// System.out.println(cursor.next());
									HashMap<String, ByteIterator> vals = new HashMap<String, ByteIterator>();
									DBObject curs = cursor.next();
									if (curs.get("tpic") != null) {
										byte[] myPic = (byte[]) curs
												.get("tpic");
										vals.put("tpic",
												new ObjectByteIterator(myPic));
										if (testMode) {
											// Save loaded image from database
											// into new image file
											FileOutputStream fos = new FileOutputStream(
													profileOwnerID
													+ "-"
													+ cnt
													+ "-mthumbimage.bmp");
											fos.write(myPic);
											fos.close();
										}
										curs.removeField("tpic");
									}
									vals.putAll(curs.toMap());
									vals.remove("ConfFriends");
									vals.remove("PendFriends");
									vals.remove("wallResources");
									// needed to do this so the coreworkload
									// will not need to know the datastore typr
									if (vals.get("_id") != null) {
										String tmp = vals.get("_id") + "";
										vals.remove("_id");
										vals.put(
												"userid",
												new ObjectByteIterator(tmp
														.getBytes()));
									}
									result.add(vals);
								}
								cursor.close();
								// empty list
								list.clear();
							}
						}
					} else if (friendListReq) {// retrive one list
						List<Integer> list = new ArrayList<Integer>();
						for (int i = 0; i < friendIds.length; i++) {
							// put all in one list and retrieve instead of
							// retrieving one by one
							list.add(Integer.parseInt(friendIds[i].trim()));
						}
						query.put("_id", new BasicDBObject("$in", list));
						query.removeField("pic");
						// DBCursor cursor = collection.find(query, fieldsObj);
						DBCursor cursor = collection.find(query, attrs);

						int cnt = 0;
						while (cursor.hasNext()) {
							cnt++;
							HashMap<String, ByteIterator> vals = new HashMap<String, ByteIterator>();
							DBObject curs = cursor.next();
							if (curs.get("tpic") != null) {
								byte[] myPic = (byte[]) curs.get("tpic");
								vals.put("tpic", new ObjectByteIterator(myPic));
								if (testMode) {
									// Save loaded image from database into new
									// image file
									FileOutputStream fos = new FileOutputStream(
											profileOwnerID + "-" + cnt
											+ "-mthumbimage.bmp");
									fos.write(myPic);
									fos.close();
								}
								curs.removeField("tpic");
							}

							vals.putAll(curs.toMap());
							vals.remove("ConfFriends");
							vals.remove("PendFriends");
							vals.remove("wallResurces");
							// needed to do this so the coreworkload will not
							// need to know the datastore typr
							if (vals.get("_id") != null) {
								String tmp = vals.get("_id") + "";
								vals.remove("_id");
								vals.put("userid",
										new ObjectByteIterator(tmp.getBytes()));
							}
							result.add(vals);
						}
						cursor.close();
					}
				}
			}

		} catch (Exception e) {
			System.out.println(e.toString());
			retVal = -1;
		} finally {
			if (db != null) {
				db.requestDone();
			}
		}
		return retVal;

	}

	@Override
	public int viewFriendReq(int profileOwnerID,
			Vector<HashMap<String, ByteIterator>> values, boolean insertImage,
			boolean testMode) {
		int retVal = 0;
		if (profileOwnerID < 0)
			return -1;

		// first get all pending friendids for profileOwnerID
		com.mongodb.DB db = null;

		try {
			db = mongo.getDB(database);
			db.requestStart();
			DBCollection collection = db.getCollection("users");
			DBObject q = new BasicDBObject().append("_id", profileOwnerID);
			q.removeField("pic");
			BasicDBObject fields = new BasicDBObject("pic", 0);

			DBObject queryResult = null;
			queryResult = collection.findOne(q, fields);

			String x = queryResult.get("PendFriends").toString();
			if (!x.equals("")) {
				x = x.substring(2, x.length() - 1);
				if (!x.equals("")) {
					String friendIds[] = x.split(",");
					BasicDBObject query = new BasicDBObject();

					List<Integer> list = new ArrayList<Integer>();
					if (!friendListReq) {
						int cnt = 0;
						for (int i = 0; i < friendIds.length; i++) {
							cnt++;
							HashMap<String, ByteIterator> vals = new HashMap<String, ByteIterator>();
							DBObject frnd = new BasicDBObject().append("_id",
									Integer.parseInt(friendIds[i].trim()));
							frnd.removeField("pic");
							DBObject frndQueryResult = null;
							frndQueryResult = collection.findOne(frnd, fields);
							if (frndQueryResult.get("tpic") != null) {
								byte[] myPic = (byte[]) frndQueryResult
										.get("tpic");
								vals.put("tpic", new ObjectByteIterator(myPic));
								if (testMode) {
									// Save loaded image from database into new
									// image file
									FileOutputStream fos = new FileOutputStream(
											profileOwnerID + "-" + i
											+ "-mthumbimage.bmp");
									fos.write(myPic);
									fos.close();
								}
								frndQueryResult.removeField("tpic");
							}
							if (frndQueryResult != null) {
								frndQueryResult.removeField("ConfFriends");
								frndQueryResult.removeField("PendFriends");
								frndQueryResult.removeField("wallResources");
								vals.putAll(frndQueryResult.toMap());
							}
							if (vals.get("_id") != null) {
								String tmp = vals.get("_id") + "";
								vals.remove("_id");
								vals.put("userid",
										new ObjectByteIterator(tmp.getBytes()));
							}
							values.add(vals);
						}
					} else if (friendListReq) {// retrive one list
						for (int i = 0; i < friendIds.length; i++) {
							// put all in one list and retrieve instead of
							// retrieving one by one
							list.add(Integer.parseInt(friendIds[i].trim()));
						}
						query.put("_id", new BasicDBObject("$in", list));
						query.removeField("pic");
						DBCursor cursor = collection.find(query, fields);
						int cnt = 0;
						while (cursor.hasNext()) {
							cnt++;
							// System.out.println(cursor.next());
							HashMap<String, ByteIterator> vals = new HashMap<String, ByteIterator>();
							DBObject curs = cursor.next();
							if (curs.get("tpic") != null) {
								byte[] myPic = (byte[]) curs.get("tpic");
								vals.put("tpic", new ObjectByteIterator(myPic));
								if (testMode) {
									// Save loaded image from database into new
									// image file
									FileOutputStream fos = new FileOutputStream(
											profileOwnerID + "-" + cnt
											+ "-mthumbimage.bmp");
									fos.write(myPic);
									fos.close();
								}
								curs.removeField("tpic");
							}
							vals.putAll(curs.toMap());
							vals.remove("PendFriends");
							vals.remove("ConfFriends");
							vals.remove("wallResources");
							// needed to do this so the coreworkload will not
							// need to know the datastore typr
							if (vals.get("_id") != null) {
								String tmp = vals.get("_id") + "";
								vals.remove("_id");
								vals.put("userid",
										new ObjectByteIterator(tmp.getBytes()));
							}
							values.add(vals);
						}
						cursor.close();
					}

				}

			}

		} catch (Exception e) {
			System.out.println(e.toString());
			retVal = -1;
		} finally {
			if (db != null) {
				db.requestDone();
			}
		}
		return retVal;

	}

	@Override
	public int acceptFriend(int invitorID, int inviteeID) {
		// delete from pending of the invitee
		// add to confirmed of both invitee and invitor
		int retVal = 0;
		if (invitorID < 0 || inviteeID < 0)
			return -1;

		com.mongodb.DB db = null;
		try {
			db = mongo.getDB(database);
			db.requestStart();
			DBCollection collection = db.getCollection("users");
			DBObject q = new BasicDBObject().append("_id", inviteeID);

			// pull out of invitees pending
			BasicDBObject updateCommand = new BasicDBObject();
			updateCommand.put("$pull", new BasicDBObject("PendFriends",
					invitorID));
			WriteResult res = collection.update(q, updateCommand, false, false,
					writeConcern);

			// add to invitees confirmed
			updateCommand = new BasicDBObject();
			updateCommand.put("$push", new BasicDBObject("ConfFriends",
					invitorID));
			res = collection.update(q, updateCommand, false, false,
					writeConcern);

			// add to invitore confirmed
			q = new BasicDBObject().append("_id", invitorID);
			updateCommand = new BasicDBObject();
			updateCommand.put("$push", new BasicDBObject("ConfFriends",
					inviteeID));
			res = collection.update(q, updateCommand, false, false,
					writeConcern);
			db.requestDone();
			return res.getN() == 1 ? 0 : -1;

		} catch (Exception e) {
			System.out.println(e.toString());
			retVal = -1;
		} finally {
			if (db != null) {
				db.requestDone();
			}
		}
		return retVal;

	}

	@Override
	public int rejectFriend(int invitorID, int inviteeID) {
		// remove from pending of invitee
		int retVal = 0;
		if (invitorID < 0 || inviteeID < 0)
			return -1;

		com.mongodb.DB db = null;
		try {
			db = mongo.getDB(database);
			db.requestStart();
			DBCollection collection = db.getCollection("users");
			DBObject q = new BasicDBObject().append("_id", inviteeID);

			// pull out of invitees pending
			BasicDBObject updateCommand = new BasicDBObject();
			updateCommand.put("$pull", new BasicDBObject("PendFriends",
					invitorID));
			WriteResult res = collection.update(q, updateCommand, false, false,
					writeConcern);
			db.requestDone();
			return res.getN() == 1 ? 0 : -1;
		} catch (Exception e) {
			System.out.println(e.toString());
			retVal = -1;
		} finally {
			if (db != null) {
				db.requestDone();
			}
		}
		return retVal;
	}

	@Override
	public int inviteFriend(int invitorID, int inviteeID) {
		// add to pending for the invitee
		int retVal = 0;
		if (invitorID < 0 || inviteeID < 0)
			return -1;
		com.mongodb.DB db = null;
		try {
			db = mongo.getDB(database);

			db.requestStart();
			DBCollection collection = db.getCollection("users");
			DBObject q = new BasicDBObject().append("_id", inviteeID);

			BasicDBObject updateCommand = new BasicDBObject();
			updateCommand.put("$push", new BasicDBObject("PendFriends",
					invitorID));
			WriteResult res = collection.update(q, updateCommand, false, false,
					writeConcern);
		} catch (Exception e) {
			System.out.println(e.toString());
			retVal = -1;
		} finally {
			if (db != null) {
				db.requestDone();
			}
		}
		return retVal;
	}

	@Override
	public int viewTopKResources(int requesterID, int profileOwnerID, int k,
			Vector<HashMap<String, ByteIterator>> result) {
		int retVal = 0;
		if (profileOwnerID < 0 || requesterID < 0 || k < 0)
			return -1;

		com.mongodb.DB db = null;

		try {
			db = mongo.getDB(database);
			db.requestStart();
			if (scanResources) {
				DBCollection collection = db.getCollection("resources");
				// find all resources that belong to profileOwnerID
				// sort them by _id desc coz we want latest ones and get the top
				// k
				DBObject q = new BasicDBObject().append("walluserid",
						Integer.toString(profileOwnerID));
				DBCursor queryResult = null;
				queryResult = collection.find(q);
				// DBObject s = new BasicDBObject().append("_id", -1); //desc
				DBObject s = new BasicDBObject(); // desc
				s.put("_id", -1);
				queryResult = queryResult.sort(s);
				queryResult = queryResult.limit(k);
				Iterator it = queryResult.iterator();
				while (it.hasNext()) {
					HashMap<String, ByteIterator> vals = new HashMap<String, ByteIterator>();
					DBObject oneRes = new BasicDBObject();
					oneRes.putAll((DBObject) it.next());
					vals.putAll(oneRes.toMap());
					// needed to do this so the coreworkload will not need to
					// know the datastore type
					if (vals.get("_id") != null) {
						String tmp = vals.get("_id") + "";
						vals.remove("_id");
						vals.put("rid", new ObjectByteIterator(tmp.getBytes()));
					}
					if (vals.get("walluserid") != null) {
						String tmp = vals.get("walluserid") + "";
						vals.remove("walluserid");
						vals.put("walluserid",
								new ObjectByteIterator(tmp.getBytes()));
					}
					if (vals.get("creatorid") != null) {
						String tmp = vals.get("creatorid") + "";
						vals.remove("creatorid");
						vals.put("creatorid",
								new ObjectByteIterator(tmp.getBytes()));
					}
					result.add(vals);
				}
				queryResult.close();

			} else {
				DBCollection collection = db.getCollection("users");
				DBObject q = new BasicDBObject().append("_id", profileOwnerID);
				DBObject queryResult = null;
				queryResult = collection.findOne(q);
				String x = queryResult.get("wallResources").toString();
				if (!x.equals("[ ]")) {
					x = x.substring(1, x.length() - 1);
					String resourceIds[] = x.split(",");
					BasicDBObject query = new BasicDBObject();
					List<Integer> list = new ArrayList<Integer>();
					for (int i = resourceIds.length - 1; i >= resourceIds.length
							- Math.min(k, resourceIds.length); i--) { // to
						// limit
						// it
						list.add(Integer.parseInt(resourceIds[i].trim()));
					}
					collection = db.getCollection("resources");
					query.put("_id", new BasicDBObject("$in", list));
					DBCursor cursor = collection.find(query);
					while (cursor.hasNext()) {
						HashMap<String, ByteIterator> vals = new HashMap<String, ByteIterator>();
						vals.putAll(cursor.next().toMap());
						if (vals.get("_id") != null) {
							String tmp = vals.get("_id") + "";
							vals.remove("_id");
							vals.put("rid",
									new ObjectByteIterator(tmp.getBytes()));
						}
						if (vals.get("walluserid") != null) {
							String tmp = vals.get("walluserid") + "";
							vals.remove("walluserid");
							vals.put("walluserid",
									new ObjectByteIterator(tmp.getBytes()));
						}
						if (vals.get("creatorid") != null) {
							String tmp = vals.get("creatorid") + "";
							vals.remove("creatorid");
							vals.put("creatorid",
									new ObjectByteIterator(tmp.getBytes()));
						}
						result.add(vals);
					}
					cursor.close();
				}
			}
		} catch (Exception e) {
			System.out.println(e.toString());
			retVal = -1;
		} finally {
			if (db != null) {
				db.requestDone();
			}
		}
		return retVal;

	}

	public int getCreatedResources(int creatorID,
			Vector<HashMap<String, ByteIterator>> result) {
		int retVal = 0;
		if (creatorID < 0)
			return -1;

		com.mongodb.DB db = null;

		try {
			db = mongo.getDB(database);
			db.requestStart();
			DBCollection collection = db.getCollection("resources");
			// find all resources that belong to profileOwnerID
			// sort them by _id desc coz we want latest ones and get the top k
			DBObject q = new BasicDBObject().append("creatorid",
					Integer.toString(creatorID));
			DBCursor queryResult = null;
			queryResult = collection.find(q);
			// DBObject s = new BasicDBObject().append("_id", -1); //desc
			DBObject s = new BasicDBObject(); // desc
			s.put("_id", -1);
			queryResult = queryResult.sort(s);
			Iterator it = queryResult.iterator();
			while (it.hasNext()) {
				HashMap<String, ByteIterator> vals = new HashMap<String, ByteIterator>();
				DBObject oneRes = new BasicDBObject();
				oneRes.putAll((DBObject) it.next());
				vals.putAll(oneRes.toMap());
				// needed to do this so the coreworkload will not need to know
				// the datastore typr
				if (vals.get("_id") != null) {
					String tmp = vals.get("_id") + "";
					vals.remove("_id");
					vals.put("rid", new ObjectByteIterator(tmp.getBytes()));
				}
				if (vals.get("creatorid") != null) {
					String tmp = vals.get("creatorid") + "";
					vals.remove("creatorid");
					vals.put("creatorid",
							new ObjectByteIterator(tmp.getBytes()));
				}
				result.add(vals);
			}
			queryResult.close();

		} catch (Exception e) {
			System.out.println(e.toString());
			retVal = -1;
		} finally {
			if (db != null) {
				db.requestDone();
			}
		}
		return retVal;

	}

	@Override
	public int viewCommentOnResource(int requesterID, int profileOwnerID,
			int resourceID, Vector<HashMap<String, ByteIterator>> result) {
		int retVal = 0;
		if (profileOwnerID < 0 || requesterID < 0 || resourceID < 0)
			return -1;

		com.mongodb.DB db = null;

		try {
			db = mongo.getDB(database);
			db.requestStart();
			if (!manipulationArray) {
				DBCollection collection = db.getCollection("manipulation");
				// find all resources that belong to profileOwnerID
				// sort them by _id desc coz we want latest ones and get the top
				// k
				DBObject q = new BasicDBObject().append("rid",
						Integer.toString(resourceID));
				DBCursor queryResult = null;
				queryResult = collection.find(q);
				Iterator<DBObject> it = queryResult.iterator();
				while (it.hasNext()) {
					HashMap<String, ByteIterator> vals = new HashMap<String, ByteIterator>();
					DBObject oneRes = new BasicDBObject();
					oneRes.putAll((DBObject) it.next());
					vals.putAll(oneRes.toMap());
					result.add(vals);
				}
				queryResult.close();
			} else {
				DBCollection collection = db.getCollection("resources");
				DBObject q = new BasicDBObject().append("_id", resourceID);
				DBObject queryResult = null;
				queryResult = collection.findOne(q);
				if (queryResult.get("Manipulations") != ""
						&& queryResult.get("Manipulations") != null) {
					ArrayList<DBObject> mans = (ArrayList<DBObject>) queryResult
							.get("Manipulations");
					for (int i = 0; i < mans.size(); i++) {
						result.add((HashMap<String, ByteIterator>) mans.get(i)
								.toMap());
					}
				}
			}

		} catch (Exception e) {
			System.out.println(e.toString());
			retVal = -1;
		} finally {
			if (db != null) {
				db.requestDone();
			}
		}
		return retVal;
	}

	@Override
	public int postCommentOnResource(int commentCreatorID, int profileOwnerID,
			int resourceID, HashMap<String, ByteIterator> commentValues) {
		int retVal = 0;
		if (profileOwnerID < 0 || commentCreatorID < 0 || resourceID < 0)
			return -1;
		// create a new document
		com.mongodb.DB db = null;
		try {
			// get the appropriate database
			db = mongo.getDB(database);
			db.requestStart();
			if (!manipulationArray) { // consider a separate manipoulations
				// table
				DBCollection collection = db.getCollection("manipulation");
				// create the row-object-document
				// need to insert key as integer else the sorting based on id
				// for topk s wont happen properly
				HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();
				values.put("mid", commentValues.get("mid"));
				values.put("creatorid", new ObjectByteIterator(Integer
						.toString(profileOwnerID).getBytes()));
				values.put("rid",
						new ObjectByteIterator(Integer.toString(resourceID)
								.getBytes()));
				values.put("modifierid", new ObjectByteIterator(Integer
						.toString(commentCreatorID).getBytes()));
				values.put("timestamp", commentValues.get("timestamp"));
				values.put("type", commentValues.get("type"));
				values.put("content", commentValues.get("content"));

				DBObject r = new BasicDBObject();
				for (String k : values.keySet()) {
					r.put(k, values.get(k).toString());
				}

				WriteResult res = collection.insert(r, writeConcern);
				return res.getError() == null ? 0 : -1;
			} else {
				// second approach - store manipulations as elements in an array
				// for resource
				HashMap<String, String> sVals = new HashMap<String, String>();
				sVals.put("mid", commentValues.get("mid").toString());
				sVals.put("creatorid", Integer.toString(profileOwnerID));
				sVals.put("rid", Integer.toString(resourceID));
				sVals.put("modifierid", Integer.toString(commentCreatorID));
				sVals.put("timestamp", commentValues.get("timestamp")
						.toString());
				sVals.put("type", commentValues.get("type").toString());
				sVals.put("content", commentValues.get("content").toString());
				DBCollection collection = db.getCollection("resources");
				DBObject q = new BasicDBObject().append("_id", resourceID);

				BasicDBObject updateCommand = new BasicDBObject();
				updateCommand.put("$push", new BasicDBObject("Manipulations",
						sVals));
				WriteResult res = collection.update(q, updateCommand, false,
						false, writeConcern);
				db.requestDone();
				return res.getError() == null ? 0 : -1;
			}

		} catch (Exception e) {
			System.out.println(e.toString());
			retVal = -1;
		} finally {
			if (db != null) {
				db.requestDone();
			}
		}
		return retVal;
	}

	@Override
	public int delCommentOnResource(int resourceCreatorID, int resourceID,
			int manipulationID) {
		int retVal = 0;
		if (resourceCreatorID < 0 || resourceID < 0 || manipulationID < 0)
			return -1;
		com.mongodb.DB db = null;
		try {
			// get the appropriate database
			db = mongo.getDB(database);
			db.requestStart();
			if (!manipulationArray) { // consider a separate manipoulations
				// table
				DBCollection collection = db.getCollection("manipulation");
				DBObject q = new BasicDBObject().append("mid",
						Integer.toString(manipulationID)).append("rid",
								Integer.toString(resourceID));

				collection.remove(q);

			} else {
				DBCollection collection = db.getCollection("resources");
				DBObject q = new BasicDBObject().append("_id", resourceID);
				BasicDBObject updateCommand = new BasicDBObject(
						"Manipulations", new BasicDBObject("mid",
								Integer.toString(manipulationID)));
				WriteResult res = collection.update(q, new BasicDBObject(
						"$pull", updateCommand), false, false, writeConcern);
				if (res.getN() != 1)
					return -1;
			}

		} catch (Exception e) {
			System.out.println(e.toString());
			retVal = -1;
		} finally {
			if (db != null) {
				db.requestDone();
			}
		}
		return retVal;
	}

	@Override
	public int thawFriendship(int friendid1, int friendid2) {
		// delete from both their confFriends
		int retVal = 0;
		if (friendid1 < 0 || friendid2 < 0)
			return -1;

		com.mongodb.DB db = null;
		try {
			db = mongo.getDB(database);
			db.requestStart();
			DBCollection collection = db.getCollection("users");
			DBObject q = new BasicDBObject().append("_id", friendid1);

			// pull out of friend1
			BasicDBObject updateCommand = new BasicDBObject();
			updateCommand.put("$pull", new BasicDBObject("ConfFriends",
					friendid2));
			WriteResult res = collection.update(q, updateCommand, false, false,
					writeConcern);
			if (res.getN() != 1)
				return -1;

			q = new BasicDBObject().append("_id", friendid2);
			// pull out of friendid2
			updateCommand = new BasicDBObject();
			updateCommand.put("$pull", new BasicDBObject("ConfFriends",
					friendid1));
			res = collection.update(q, updateCommand, false, false,
					writeConcern);

			db.requestDone();
			return res.getN() == 1 ? 0 : -1;

		} catch (Exception e) {
			System.out.println(e.toString());
			retVal = -1;
		} finally {
			if (db != null) {
				db.requestDone();
			}
		}
		return retVal;
	}

	@Override
	public HashMap<String, String> getInitialStats() {

		HashMap<String, String> stats = new HashMap<String, String>();
		com.mongodb.DB db = null;
		try {
			db = mongo.getDB(database);
			db.requestStart();
			// get the number of users
			DBCollection collection = db.getCollection("users");
			DBCursor users = collection.find();
			int usercnt = users.count();
			users.close();
			stats.put("usercount", Integer.toString(usercnt));

			// find user offset
			DBObject m = new BasicDBObject().append("_id", 1);
			DBCursor minUser = collection.find(m).limit(1);
			int offset = 0;
			if (minUser.hasNext())
				offset = (Integer) minUser.next().toMap().get("_id");
			minUser.close();
			// get the number of friends per user
			DBObject q = new BasicDBObject().append("_id", offset);
			DBObject queryResult = null;
			queryResult = collection.findOne(q);
			String x = queryResult.get("ConfFriends").toString();
			int frndCount = 0;
			if (x.equals("")
					|| (!x.equals("") && (x.substring(2, x.length() - 1))
							.equals("")))
				frndCount = 0;
			else {
				x = x.substring(2, x.length() - 1);
				frndCount = x.split(",").length;
			}
			stats.put("avgfriendsperuser", Integer.toString(frndCount));

			x = queryResult.get("PendFriends").toString();
			int pendCount = 0;
			if (x.equals("")
					|| (!x.equals("") && (x.substring(2, x.length() - 1))
							.equals("")))
				pendCount = 0;
			else {
				x = x.substring(2, x.length() - 1);
				pendCount = x.split(",").length;
			}
			stats.put("avgpendingperuser", Integer.toString(pendCount));

			// find number of resources for the user
			DBCollection resCollection = db.getCollection("resources");
			DBObject res = new BasicDBObject().append("creatorid",
					Integer.toString(offset));
			DBCursor resQueryResult = null;
			resQueryResult = resCollection.find(res);
			int resCount = resQueryResult.count();
			resQueryResult.close();
			stats.put("resourcesperuser", Integer.toString(resCount));
		} catch (Exception e) {
			e.printStackTrace(System.out);
		} finally {
			if (db != null)
				db.requestDone();
		}
		return stats;
	}

	public void cleanup(boolean warmup) throws DBException {
		if(!warmup){
			try {
				crtcl.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			decrementNumThreads();
			// add instance to vector of connections
			if (NumThreads.get() > 0) {
				crtcl.release();
				return;
			} else {
				// close all connections in vector
				try {
					Thread.sleep(6000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(mongo!= null) mongo.close();
				crtcl.release();
			}
		}

	}

	@Override
	public int CreateFriendship(int memberA, int memberB) {
		int retVal = acceptFriend(memberA, memberB);
		return retVal;
	}

	@Override
	public int queryPendingFriendshipIds(int profileId,
			Vector<Integer> pendingFrnds) {
		int retVal = 0;
		com.mongodb.DB db = null;
		try {
			db = mongo.getDB(database);
			db.requestStart();
			DBCollection collection = db.getCollection("users");
			DBObject q = new BasicDBObject().append("_id", profileId);
			DBObject queryResult = null;
			queryResult = collection.findOne(q);
			String x = queryResult.get("PendFriends").toString();
			if (!x.equals("")) {
				x = x.substring(2, x.length() - 1);
				if (!x.equals("")) {
					String friendIds[] = x.split(",");
					for (int j = 0; j < friendIds.length; j++)
						pendingFrnds.add(Integer.parseInt(friendIds[j].trim()));
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
			retVal = -1;
		}
		return retVal;
	}

	@Override
	public int queryConfirmedFriendshipIds(int profileId,
			Vector<Integer> confFrnds) {
		int retVal = 0;
		com.mongodb.DB db = null;
		try {
			db = mongo.getDB(database);
			db.requestStart();
			DBCollection collection = db.getCollection("users");
			DBObject q = new BasicDBObject().append("_id", profileId);
			DBObject queryResult = null;
			queryResult = collection.findOne(q);
			String x = queryResult.get("ConfFriends").toString();
			if (!x.equals("")) {
				x = x.substring(2, x.length() - 1);
				if (!x.equals("")) {
					String friendIds[] = x.split(",");
					for (int j = 0; j < friendIds.length; j++)
						confFrnds.add(Integer.parseInt(friendIds[j].trim()));
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
			retVal = -1;
		}
		return retVal;
	}


	@Override
	public void createSchema(Properties props) {

		// drop all collections
		com.mongodb.DB db = null;
		try {
			// drop database and collections
			// db = mongo.getDB(database);
			db = mongo.getDB("admin");
			db.requestStart();
			// add shards
			/*
			 * BasicDBObject addShard = new BasicDBObject("addshard",
			 * "10.0.0.138:27011"); CommandResult shardAddResult =
			 * db.command(addShard); shardAddResult.getException();
			 * System.out.println(shardAddResult.toString()); addShard = new
			 * BasicDBObject("addshard", "10.0.0.138:27012"); shardAddResult =
			 * db.command(addShard);
			 * System.out.println(shardAddResult.toString()); //db.command(
			 * " { addshard : \"10.0.0.138:27011\" , allowLocal : true } )");
			 */

			db = mongo.getDB(database);
			DBCollection collection = db.getCollection("users");
			collection.drop();
			collection = db.getCollection("resources");
			collection.drop();
			collection = db.getCollection("manipulation");
			collection.drop();

			if (Boolean.parseBoolean(props.getProperty(
					MONGODB_SHARDING_PROPERTY,
					MONGODB_SHARDING_PROPERTY_DEFAULT)) == true) {
				// enable sharding on the database in the admin user
				db = mongo.getDB("admin");
				BasicDBObject s = new BasicDBObject("enablesharding",
						props.getProperty(MONGODB_DB_PROPERTY, "benchmark"));
				CommandResult cr = db.command(s);

				// enable sharding on each collection
				cr = db.command(BasicDBObjectBuilder
						.start("shardCollection", "benchmark.users")
						.push("key").add("_id", 1).pop().get());
				if (Boolean.parseBoolean(props.getProperty(
						MONGODB_MANIPULATION_ARRAY_PROPERTY,
						MONGODB_MANIPULATION_ARRAY_PROPERTY_DEFAULT)) == false) {
					cr = db.command(BasicDBObjectBuilder
							.start("shardCollection", "benchmark.resources")
							.push("key").add("walluserid", 1).pop().get());
					cr = db.command(BasicDBObjectBuilder
							.start("shardCollection", "benchmark.manipulation")
							.push("key").add("rid", 1).pop().get());
				} else {
					cr = db.command(BasicDBObjectBuilder
							.start("shardCollection", "benchmark.resources")
							.push("key").add("_id", 1).pop().get());
				}
				//force move a chunk	
				//BasicDBObject mov = new BasicDBObject("moveChunk","benchmark.users");
				//BasicDBObject mo2 = new BasicDBObject("_id", 1); //this is the chunkid not the userid
				//mov.put("find", mo2);
				//mov.put("to", "shard0001");
				//CommandResult result2 = mongo.getDB("admin").command(mov);
				//System.out.println(result2);
				//check if migration is happening
//				com.mongodb.DB db2 = mongo.getDB("config");
//				DBCollection cl = db2.getCollection("locks");
//				DBObject q = new BasicDBObject().append("_id", "balancer");
//				DBObject qres = cl.findOne(q);
//				while( Integer.parseInt(qres.get("state").toString()) == 1 )
//					System.out.println("still migrating");
			}

			// create indexes on collection
			db = mongo.getDB(database);
			collection = db.getCollection("users");
			collection.createIndex(new BasicDBObject("users.wallResources", 1)); // create
			// index
			// on
			collection = db.getCollection("resources");
			collection.createIndex(new BasicDBObject("walluserid", 1)); // create
			// index
			// on
			// "i",
			// ascending
			if (Boolean.parseBoolean(props.getProperty(
					MONGODB_MANIPULATION_ARRAY_PROPERTY,
					MONGODB_MANIPULATION_ARRAY_PROPERTY_DEFAULT)) == false) {
				collection = db.getCollection("manipulation");
				collection.createIndex(new BasicDBObject("mid", 1));
				collection.createIndex(new BasicDBObject("rid", 1)); // create
				// index
				// on
				// "i",
				// ascending
			} else {
				collection.createIndex(new BasicDBObject(
						"resources.Manipulations", 1)); // create index on
				// "i", ascending
			}

		} catch (Exception e) {
			System.out.println(e.toString());
			return;
		} finally {
			if (db != null) {
				db.requestDone();
			}
		}
	}


	@Override
	public boolean schemaCreated() {
		// query for some user and see if it exists
		com.mongodb.DB db = null;
		try {
			db = mongo.getDB(database);
			db.requestStart();
			DBCollection collection = db.getCollection("users");
			DBObject q = new BasicDBObject().append("_id", 0);
			DBObject queryResult = null;
			queryResult = collection.findOne(q);
			db.requestDone();
			if (queryResult != null) {
				return true;
			}
		} catch (Exception ex) {
			db.requestDone();
			ex.printStackTrace();
			return false;
		}
		return false;
	}

	@Override
	public void reconstructSchema() {
		com.mongodb.DB db = null;
		try {
			db = mongo.getDB(database);
			db.requestStart();
			// getting the number of users
			DBCollection collection = db.getCollection("users");
			int numUsers = (int) collection.getCount();
			// for every user set their conf Friends and pend Friends to null
			for (int i = 0; i < numUsers; i++) {
				DBObject r = new BasicDBObject().append("_id", i);
				DBObject queryResult = collection.findOne(r);
				BasicDBObject updateCommand = new BasicDBObject();
				updateCommand.put("$set", new BasicDBObject("ConfFriends",
						new ArrayList<Integer>()));
				WriteResult res = collection.update(r, updateCommand, false,
						false, writeConcern);
				updateCommand = new BasicDBObject();
				updateCommand.put("$set", new BasicDBObject("PendFriends",
						new ArrayList<Integer>()));
				res = collection.update(r, updateCommand, false, false,
						writeConcern);
			}
			 if(manipulationArray){
				 collection = db.getCollection("resources");
				 int numResources = (int) collection.getCount();
					// for every user set their conf Friends and pend Friends to null
					for (int i = 0; i < numUsers; i++) {
						DBObject r = new BasicDBObject().append("_id", i);
						DBObject queryResult = collection.findOne(r);
						BasicDBObject updateCommand = new BasicDBObject();
						updateCommand.put("$set", new BasicDBObject("Manipulations",
								new ArrayList<Integer>()));
						WriteResult res = collection.update(r, updateCommand, false,
								false, writeConcern);
					}
			 }else{
				 collection = db.getCollection("manipulation");
				 //TODO: test this
					if (Boolean.parseBoolean(props.getProperty(
							MONGODB_SHARDING_PROPERTY,
							MONGODB_SHARDING_PROPERTY_DEFAULT)) == true) 
						collection.drop();  //this doesnt work with shards
					else{
						DBCursor DBCur = collection.find();
						while(DBCur.hasNext())
							collection.remove(DBCur.next());
					}
				 
			 }
			
			db.requestDone();
		} catch (Exception ex) {
			db.requestDone();
			ex.printStackTrace();
		}
	}

	@Override
	public boolean dataAvailable(){
		com.mongodb.DB db=null;
		Properties props = getProperties();
		try {
			db = mongo.getDB(database);

			//getting the number of users
			db.requestStart();				
			DBCollection collection = db.getCollection("users");
			int numUsers = (int) collection.getCount();
			if(numUsers != Integer.parseInt(props.getProperty(Client.USER_COUNT_PROPERTY, Client.USER_COUNT_PROPERTY_DEFAULT))){
				db.requestDone();
				return false;
			}
			db.requestDone();

			//getting the number of resources
			//getting the number of users
			db.requestStart();				
			collection = db.getCollection("resources");
			int numResources = (int) collection.getCount();
			if(numResources != 
					(  Integer.parseInt(props.getProperty(Client.USER_COUNT_PROPERTY, Client.USER_COUNT_PROPERTY_DEFAULT))
							* Integer.parseInt(props.getProperty(Client.RESOURCE_COUNT_PROPERTY, Client.RESOURCE_COUNT_PROPERTY_DEFAULT)) )){
				db.requestDone();
				return false;
			}
			db.requestDone();
		}catch(Exception ex){
			db.requestDone();
			ex.printStackTrace();
			return false;
		}
		return true;
	}
}