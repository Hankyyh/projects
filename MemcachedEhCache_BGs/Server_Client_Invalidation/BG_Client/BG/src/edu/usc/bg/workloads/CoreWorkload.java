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

package edu.usc.bg.workloads;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import edu.usc.bg.BGMainClass;
import edu.usc.bg.Member;
import edu.usc.bg.base.ByteIterator;
import edu.usc.bg.base.Client;
import edu.usc.bg.base.DB;
import edu.usc.bg.base.DBException;
import edu.usc.bg.base.DBFactory;
import edu.usc.bg.base.ObjectByteIterator;
import edu.usc.bg.base.RandomByteIterator;
import edu.usc.bg.base.UnknownDBException;
import edu.usc.bg.base.Workload;
import edu.usc.bg.base.WorkloadException;
import edu.usc.bg.base.generator.CounterGenerator;
import edu.usc.bg.base.generator.DiscreteGenerator;
import edu.usc.bg.base.generator.IntegerGenerator;
import edu.usc.bg.base.generator.ScrambledZipfianGenerator;
import edu.usc.bg.base.generator.SkewedLatestGenerator;
import edu.usc.bg.base.generator.UniformIntegerGenerator;
import edu.usc.bg.generator.DistOfAccess;
import edu.usc.bg.generator.Fragmentation;




/**
 * Queries the initial status of the data store
 * This thread is activated in the benchmark phase (which is initiated with the -t flag), only if the INIT_STATS_REQ_APPROACH_PROPERTY property is set
 * The status collected here is used for generating invitations without facing integrity constraint issues and validation
 * @author barahman
 *
 */
class initQueryThread extends Thread{
	DB _db;
	int[] _tMembers;
	Properties _props ;
	HashMap<Integer, Vector<Integer>> _pIds ;
	HashMap<Integer,Vector<Integer>> _cIds ;
	HashMap<Integer, Vector<Integer>> _rIds ;
	HashMap<String, Integer> _initCnt ;

	/**
	 * Initialize the thread and its connection to the data store
	 * @param tMembers
	 * @param Properties
	 */
	initQueryThread(int[] tMembers, Properties props){
		_tMembers = tMembers;
		_pIds = new HashMap<Integer, Vector<Integer>>(_tMembers.length);
		_cIds = new HashMap<Integer, Vector<Integer>>(_tMembers.length);
		_rIds = new HashMap<Integer, Vector<Integer>>(_tMembers.length);
		_initCnt = new HashMap<String, Integer>(_tMembers.length*3);

		String dbname = props.getProperty(Client.DB_CLIENT_PROPERTY, Client.DB_CLIENT_PROPERTY_DEFAULT);
		_props = props;
		try {
			_db = DBFactory.newDB(dbname, props);
			_db.init();
		} catch (UnknownDBException e) {
			System.out.println("Unknown DB, QpendingThread " + dbname);
			System.exit(0);
		} catch (DBException e) {
			e.printStackTrace(System.out);
			System.exit(0);
		}
	}

	public HashMap<String, Integer> getInit(){
		return _initCnt;
	}

	public HashMap<Integer, Vector<Integer>> getPendings(){
		return _pIds;
	}

	public HashMap<Integer, Vector<Integer>> getConfirmed(){
		return _cIds;
	}

	public HashMap<Integer, Vector<Integer>> getResources(){
		return _rIds;
	}
	
	/**
	 * clears all the structures used by this thread once the thread is done reporting its data to the main thread
	 */
	public void freeResources(){
		_tMembers = null;
		if(_cIds != null){
			_cIds.clear();
			_cIds = null;
		}

		if(_pIds != null){
			_pIds.clear();
			_pIds = null;
		}
		
		if(_rIds != null){
			_rIds.clear();
			_rIds = null;
		}

		if(_initCnt != null){
			_initCnt.clear();
			_initCnt = null;
		}
	}


	public void run(){
		int res =0;
		for(int i=0; i<_tMembers.length; i++){
			HashMap<String, ByteIterator> profileResult = new HashMap<String, ByteIterator>();
			res = _db.viewProfile(_tMembers[i], _tMembers[i], profileResult, false, false);
			if(res < 0){
				System.out.println("Problem in getting initial stats.");
				System.exit(0);	
			}	
			_initCnt.put("PENDFRND-"+_tMembers[i], Integer.parseInt(profileResult.get("pendingcount").toString().trim()));
			_initCnt.put("ACCEPTFRND-"+_tMembers[i], Integer.parseInt(profileResult.get("friendcount").toString().trim()));
			if(profileResult != null){
				profileResult.clear();
				profileResult = null;
			}

			//get all resources for this user
			getStatsForUser(i);
		}

		try {
			_db.cleanup(true);
		} catch (Exception e) {
			e.printStackTrace(System.out);
			return;
		}
	}

	/**
	 * @param i
	 */
	private void getStatsForUser(int i) {
		int resViewComment, resCreateRes, resPendingFriends, resConfirmedFriends;
		Vector<Integer> resIds = new Vector<Integer>();
		
		Vector<HashMap<String, ByteIterator>> resResult = new Vector<HashMap<String, ByteIterator>>();		
		resCreateRes = _db.getCreatedResources(_tMembers[i], resResult);

		//get pending friends to relate them
		Vector<Integer> pids = new Vector<Integer>();
		resPendingFriends = _db.queryPendingFriendshipIds(_tMembers[i], pids);

		//get confirmed friends to relate them
		Vector<Integer> cids = new Vector<Integer>();
		resConfirmedFriends = _db.queryConfirmedFriendshipIds(_tMembers[i], cids);
		
		if(resPendingFriends < 0 || resCreateRes < 0 || resConfirmedFriends < 0){
			System.out.println("Problem in getting initial stats.");
			System.exit(0);	
		}	
		for(int d=0; d<resResult.size(); d++){
			String resId = resResult.get(d).get("rid").toString().trim();
			resIds.add(Integer.parseInt(resId));
			Vector<HashMap<String, ByteIterator>> commentResult = new Vector<HashMap<String, ByteIterator>>();
			resViewComment = _db.viewCommentOnResource(_tMembers[i], _tMembers[i], Integer.parseInt(resId), commentResult);
			if(resViewComment < 0){
				System.out.println("Problem in getting initial stats.");
				System.exit(0);	
			}	
			_initCnt.put("POSTCOMMENT-"+resId, commentResult.size());
			if(commentResult != null){
				commentResult.clear();
				commentResult = null;
			}
		}
		_rIds.put(_tMembers[i], resIds);
		if(resResult != null){
			resResult.clear();
			resResult = null;
		}
		
		_pIds.put(_tMembers[i], pids);
			
		_cIds.put(_tMembers[i], cids);
	}
}

/**
 * responsible for creating the benchmarking workload for issuing the queries based on the workload file specified
 * @author barahman
 *
 */
public class CoreWorkload extends Workload
{
	/**
	 * Once set to true, BG generates log files for the actions issued
	 */
	public static  boolean enableLogging = true;
	public static final String ENABLE_LOGGING_PROPERTY = "enablelogging";
	/**
	 * needed for zipfian distributions
	 */
	public static final String ZIPF_MEAN_PROPERTY= "zipfianmean";
	public static final String ZIPF_MEAN_PROPERTY_DEFAULT = "0.27";
	/**
	 * The name of the property for the the distribution of requests across the keyspace. Options are "uniform", "zipfian" and "latest"
	 */
	public static final String REQUEST_DISTRIBUTION_PROPERTY="requestdistribution";
	/**
	 * The default distribution of requests across the keyspace
	 */
	public static final String REQUEST_DISTRIBUTION_PROPERTY_DEFAULT="uniform";
	/**
	 * Percentage users that only access their own profile property name
	 */
	public static final String GETOWNPROFILE_PROPORTION_PROPERTY="ViewSelfProfileSession";
	/**
	 * The default proportion of functionalities that are ViewSelfProfile
	 */
	public static final String GETOWNPROFILE_PROPORTION_PROPERTY_DEFAULT="1.0";
	/**
	 * Percentage users that  access their friend profile property name
	 */
	public static final String GETFRIENDPROFILE_PROPORTION_PROPERTY="ViewFrdProfileSession";
	/**
	 * The default proportion of functionalities that are ViewFrdProfile
	 */
	public static final String GETFRIENDPROFILE_PROPORTION_PROPERTY_DEFAULT="0.0";
	/**
	 * Percentage users that  post comment on their own resource
	 */
	public static final String POSTCOMMENTONRESOURCE_PROPORTION_PROPERTY="PostCmtOnResSession";
	/**
	 * The default proportion of functionalities that are postCommentOnResource
	 */
	public static final String POSTCOMMENTONRESOURCE_PROPORTION_PROPERTY_DEFAULT="0.0";
	/**
	 * Percentage users that  delete comment on their own resource
	 */
	public static final String DELCOMMENTONRESOURCE_PROPORTION_PROPERTY="DeleteCmtOnResSession";
	/**
	 * The default proportion of functionalities that are delCommentOnResource
	 */
	public static final String DELCOMMENTONRESOURCE_PROPORTION_PROPERTY_DEFAULT="0.0";
	/**
	 * Percentage users that  do the generate friendship requests in their sessions
	 */
	public static final String GENERATEFRIENDSHIP_PROPORTION_PROPERTY="InviteFrdSession";
	/**
	 * The default proportion of functionalities that are generateFriendRequest
	 */
	public static final String GENERATEFRIENDSHIP_PROPORTION_PROPERTY_DEFAULT="0.0";	
	/**
	 * Percentage users that  do the accept friendship sequence
	 */
	public static final String ACCEPTFRIENDSHIP_PROPORTION_PROPERTY="AcceptFrdReqSession";
	/**
	 * The default proportion of functionalities that are acceptFriendship
	 */
	public static final String ACCEPTFRIENDSHIP_PROPORTION_PROPERTY_DEFAULT="0.0";
	/**
	 * Percentage users that do the reject friendship sequence
	 */
	public static final String REJECTFRIENDSHIP_PROPORTION_PROPERTY="RejectFrdReqSession";
	/**
	 * The default proportion of functionalities that are rejectFriendship
	 */
	public static final String REJECTFRIENDSHIP_PROPORTION_PROPERTY_DEFAULT="0.0";
	/**
	 * Percentage users that do the unfriend friendship user session
	 */
	public static final String UNFRIEND_PROPORTION_PROPERTY="ThawFrdshipSession";
	/**
	 * The default proportion of functionalities that are unFriendFriend
	 */
	public static final String UNFRIEND_PROPORTION_PROPERTY_DEFAULT="0.0";
	/**
	 * Percentage users that do the unfriend/accept friendship sequence
	 */
	public static final String UNFRIENDACCEPTFRIENDSHIP_PROPORTION_PROPERTY="unFriendAcceptProportion";
	/**
	 * The default proportion of functionalities that are unfriend/accept
	 */
	public static final String UNFRIENDACCEPTFRIENDSHIP_PROPORTION_PROPERTY_DEFAULT="0.0";
	/**
	 * Percentage users that do the unfriend/reject friendship sequence
	 */
	public static final String UNFRIENDREJECTFRIENDSHIP_PROPORTION_PROPERTY="unFriendRejectProportion";
	/**
	 * The default proportion of functionalities that are unfriend/reject
	 */
	public static final String UNFRIENDREJECTFRIENDSHIP_PROPORTION_PROPERTY_DEFAULT="0.0";

	//individual actions
	/**
	 * Percentage users that do the getProfile action
	 */
	public static final String GETRANDOMPROFILEACTION_PROPORTION_PROPERTY="ViewProfileAction";
	/**
	 * The default proportion of getprofile action
	 */
	public static final String GETRANDOMPROFILEACTION_PROPORTION_PROPERTY_DEFAULT="0.0";
	/**
	 * Percentage users that do the listFriends action
	 */
	public static final String GETRANDOMLISTOFFRIENDSACTION_PROPORTION_PROPERTY="ListFriendsAction";
	/**
	 * The default proportion of listFriends action
	 */
	public static final String GETRANDOMLISTOFFRIENDSACTION_PROPORTION_PROPERTY_DEFAULT="0.0";
	/**
	 * Percentage users that do the getlistofpendingrequests action
	 */
	public static final String GETRANDOMLISTOFPENDINGREQUESTSACTION_PROPORTION_PROPERTY="ViewFriendReqAction";
	/**
	 * The default proportion of getlistofpendingrequests action
	 */
	public static final String GETRANDOMLISTOFPENDINGREQUESTSACTION_PROPORTION_PROPERTY_DEFAULT="0.0";
	/**
	 * Percentage users that do the inviteFriend action
	 */
	public static final String INVITEFRIENDSACTION_PROPORTION_PROPERTY="InviteFriendAction";
	/**
	 * The default proportion of inviteFriend action
	 */
	public static final String INVITEFRIENDSACTION_PROPORTION_PROPERTY_DEFAULT="0.0";
	/**
	 * Percentage users that do the acceptfriends action
	 */
	public static final String ACCEPTFRIENDSACTION_PROPORTION_PROPERTY="AcceptFriendReqAction";
	/**
	 * The default proportion of acceptfriends action
	 */
	public static final String ACCEPTFRIENDSACTION_PROPORTION_PROPERTY_DEFAULT="0.0";
	/**
	 * Percentage users that do the rejectfriends action
	 */
	public static final String REJECTFRIENDSACTION_PROPORTION_PROPERTY="RejectFriendReqAction";
	/**
	 * The default proportion of rejectfriends action
	 */
	public static final String REJECTFRIENDSACTION_PROPORTION_PROPERTY_DEFAULT="0.0";
	/**
	 * Percentage users that do the unfriendfriends action
	 */
	public static final String UNFRIENDFRIENDSACTION_PROPORTION_PROPERTY="ThawFriendshipAction";
	/**
	 * The default proportion of unfriendfriends action
	 */
	public static final String UNFRIENDFRIENDSACTION_PROPORTION_PROPERTY_DEFAULT="0.0";
	/**
	 * Percentage users that do the gettopresources action
	 */
	public static final String GETTOPRESOURCEACTION_PROPORTION_PROPERTY="ViewTopKResourcesAction";
	/**
	 * The default proportion of gettopresources action
	 */
	public static final String GETTOPRESOURCEACTION_PROPORTION_PROPERTY_DEFAULT="0.0";
	/**
	 * Percentage users that do the getcommentsonresources action
	 */
	public static final String GETCOMMENTSONRESOURCEACTION_PROPORTION_PROPERTY="ViewCommentsOnResourceAction";
	/**
	 * The default proportion of getcommentsonresources action
	 */
	public static final String GETCOMMENTSONRESOURCEACTION_PROPORTION_PROPERTY_DEFAULT="0.0";
	/**
	 * Percentage users that do the postcommentonresources action
	 */
	public static final String POSTCOMMENTONRESOURCEACTION_PROPORTION_PROPERTY="PostCommentOnResourceAction";
	/**
	 * The default proportion of postcommentonresources action
	 */
	public static final String POSTCOMMENTONRESOURCEACTION_PROPORTION_PROPERTY_DEFAULT="0.0";
	/**
	 * Percentage users that do the delcommentonresources action
	 */
	public static final String DELCOMMENTONRESOURCEACTION_PROPORTION_PROPERTY="DeleteCommentOnResourceAction";
	/**
	 * The default proportion of delcommentonresources action
	 */
	public static final String DELCOMMENTONRESOURCEACTION_PROPORTION_PROPERTY_DEFAULT="0.0";

	/**
	 * Percentage users that do the viewNewsFeed action
	 */
	public static final String VIEWNEWSFEEDACTION_PROPORTION_PROPERTY="ViewNewsFeedAction";
	/**
	 * The default proportion of viewNewsFeed action
	 */
	public static final String VIEWNEWSFEEDACTION_PROPORTION_PROPERTY_DEFAULT="0.0";

	/**
	 * keeps a track of the existence of reads in the workload
	 * if no reads have occurred or no read log files created the validation check need not happen-shared for all threads
	 */
	public static boolean readsExist = false;
	public static boolean enforceFriendship = false;
	/**
	 * Keeps a track of existence of updates in the workload
	 * if no updates have occurred or no update log files have been created the validation check need not happen-shared for all threads 
	 */
	public static boolean updatesExist = false;

	public static HashMap<String, Integer> initStats = new HashMap<String, Integer>();

	private static int numShards = 101;
	private static char[][] userStatusShards;
	private static Semaphore[] uStatSemaphores;
	//keeps a track of user frequency of access
	private static int[][] userFreqShards;
	private static Semaphore[] uFreqSemaphores;
	
	private static DistOfAccess myDist;
	private static DistOfAccess myDist2;
	
	private static Member[] myMemberObjs;	

	static Random random = new Random();
	IntegerGenerator keysequence;
	CounterGenerator transactioninsertkeysequence;
	DiscreteGenerator operationchooser;
	IntegerGenerator keychooser;
	int usercount;
	int useroffset;

	//keep a track of all related users for every user
	private HashMap<Integer, String>[] userRelations ;
	private static Semaphore rStat = new Semaphore(1, true);

	//no need to have a lock as a user can't be activated by multiple threads to accept invitations
	private static Vector<Integer>[] pendingFrnds;
	private static Semaphore withPend = new Semaphore(1, true);
	private static HashMap<Integer, Integer> usersWithpendingFrnds; //needed for enforced friendship, maintains userid and number of pending
	private static Semaphore aFrnds = new Semaphore(1, true);
	private static HashMap<Integer, String>[] acceptedFrnds ;
	HashMap<Integer, Integer> memberIdxs = new HashMap<Integer, Integer>();
	private static Vector<Integer>[] createdResources;
	private static Semaphore sCmts = new Semaphore(1, true);
	private static HashMap<Integer,Vector<Integer>> postedComments;
	private static HashMap<Integer, Integer> maxCommentIds ;

	String requestdistrib = "";
	int machineid = 0;
	int numBGClients = 1;
	double ZipfianMean = 0.27;
	
	int clusterOffset;


	/**
	 * Initialize the scenario. 
	 * Called once, in the main client thread, before any operations are started.
	 */
	public void init(Properties p, Vector<Integer> members) throws WorkloadException
	{	
		enableLogging =Boolean.parseBoolean(p.getProperty(ENABLE_LOGGING_PROPERTY,"true" ));
		initOptionChooser(p);
		
		usercount=Integer.parseInt(p.getProperty(Client.USER_COUNT_PROPERTY, Client.USER_COUNT_PROPERTY_DEFAULT));
		useroffset = Integer.parseInt(p.getProperty(Client.USER_OFFSET_PROPERTY,Client.USER_COUNT_PROPERTY_DEFAULT));
		requestdistrib=p.getProperty(REQUEST_DISTRIBUTION_PROPERTY,REQUEST_DISTRIBUTION_PROPERTY_DEFAULT);
		machineid = Client.machineid;
		enforceFriendship = Boolean.parseBoolean(p.getProperty(Client.ENFORCE_FRIENDSHIP_PROPERTY, Client.ENFORCE_FRIENDSHIP_PROPERTY_DEFAULT));

		keysequence=new CounterGenerator(useroffset);
		
		transactioninsertkeysequence=new CounterGenerator(usercount);
		long loadst = System.currentTimeMillis();
		if (requestdistrib.equals("uniform")){
			keychooser=new UniformIntegerGenerator(0,usercount-1);
			myMemberObjs = new Member[usercount];
			for(int j=0; j<usercount; j++){
				memberIdxs.put(j+useroffset, j);
				Member newMember = new Member(j+useroffset, j, (j%numShards), (j/numShards+j%numShards));
				myMemberObjs[j] = newMember;
			}
		}
		else if (requestdistrib.equals("zipfian")){
			//it does this by generating a random "next key" in part by taking the modulus over the number of keys
			//if the number of keys changes, this would shift the modulus, and we don't want that to change which keys are popular
			//so we'll actually construct the scrambled zipfian generator with a keyspace that is larger than exists at the beginning
			//of the test. that is, we'll predict the number of inserts, and tell the scrambled zipfian generator the number of existing keys
			//plus the number of predicted keys as the total keyspace. then, if the generator picks a key that hasn't been inserted yet, will
			//just ignore it and pick another key. this way, the size of the keyspace doesn't change from the perspective of the scrambled zipfian generator
			keychooser=new ScrambledZipfianGenerator(usercount);
			myMemberObjs = new Member[usercount];
			for(int j=0; j<usercount; j++){
				memberIdxs.put(j+useroffset, j);
				Member newMember = new Member(j+useroffset, j, (j%numShards), (j/numShards+j%numShards));
				myMemberObjs[j] = newMember;
			}
		}
		else if(requestdistrib.equals("dzipfian")){
			ZipfianMean = Double.parseDouble(p.getProperty(ZIPF_MEAN_PROPERTY, ZIPF_MEAN_PROPERTY_DEFAULT));
			System.out.println("Create fragments in workload init phase");
			
			//READS
			Fragmentation createFrags = new Fragmentation(usercount, 1, 0,p.getProperty("probs",""), ZipfianMean);
			myDist = createFrags.getMyDist();
			int[] myMembers = createFrags.getMyMembers();
			
			//WRITE
			Fragmentation createFrags2 = new Fragmentation(usercount, Integer.parseInt(p.getProperty(Client.NUM_BG_PROPERTY,Client.NUM_BG_PROPERTY_DEFAULT)),machineid,p.getProperty("probs",""), ZipfianMean);
			myDist2 = createFrags2.getMyDist();
			
			clusterOffset = createFrags2.getClusterOffset();

			usercount = myMembers.length;
			myMemberObjs =new Member[usercount];
			for(int j=0; j<usercount;j++){
				memberIdxs.put(myMembers[j], j);
				Member newMember = new Member(myMembers[j], j, (j%numShards), (j/numShards));
				myMemberObjs[j] = newMember;
			}

		}
		else if (requestdistrib.equals("latest")){
			keychooser=new SkewedLatestGenerator(transactioninsertkeysequence);
			myMemberObjs = new Member[usercount];
			for(int j=0; j<usercount; j++){
				memberIdxs.put(j+useroffset, j);
				Member newMember = new Member(j+useroffset, j, (j%numShards), (j/numShards+j%numShards));
				myMemberObjs[j] = newMember;
			}
		}
		else{
			throw new WorkloadException("Unknown request distribution \""+requestdistrib+"\"");
		}
		System.out.println("Time to create fragments : "+(System.currentTimeMillis()-loadst)+" msec");
		createInternalDSFragments();
	}
	
	private void createInternalDSFragments() {
		userStatusShards = new char[numShards][];
		uStatSemaphores = new Semaphore[numShards];

		userFreqShards = new int[numShards][];
		uFreqSemaphores = new Semaphore[numShards];

		int avgShardSize = usercount/numShards;
		int remainingMembers = usercount-(avgShardSize*numShards);
		for(int i=0; i<numShards; i++){
			int numShardUsers = avgShardSize;
			if(i<remainingMembers)
				numShardUsers++;
			userStatusShards[i] = new char[numShardUsers];
			userFreqShards[i] = new int[numShardUsers];
			for(int j=0; j<numShardUsers; j++){
				userStatusShards[i][j]='d';
				userFreqShards[i][j]=0;
			}
			uStatSemaphores[i] = new Semaphore(1, true);
			uFreqSemaphores[i] = new Semaphore(1, true);
		}
	}
	/**
	 * @param Properties 
	 */
	private void initOptionChooser(Properties p) {
		//sessions
		Map<String, Double> session = getSessionParameters(p);
		
		//actions
		Map<String, Double> actions = getActionParameters(p);
		
		operationchooser=new DiscreteGenerator();
		
		//sessions
		for(Map.Entry<String, Double> entry:session.entrySet()){
			if(entry.getValue()>0) {
				operationchooser.addValue(entry.getValue(), entry.getKey());
			}
		}

		//actions
		for(Map.Entry<String, Double> entry:actions.entrySet()){
			if(entry.getValue()>0) {
				operationchooser.addValue(entry.getValue(), entry.getKey());
			}
		}
	}

	/**
	 * @param Properties 
	 * return
	 */
	private Map<String, Double> getActionParameters(Properties p) {
		Map<String,Double> actions = new HashMap<String, Double>();
		final String GETPROACT = "GETPROACT";
		final String GETFRNDLSTACT = "GETFRNDLSTACT";
		final String GETPENDACT = "GETPENDACT";
		final String INVFRNDACT = "INVFRNDACT";
		final String ACCFRNDACT = "ACCFRNDACT";
		final String REJFRNDACT = "REJFRNDACT";
		final String UNFRNDACT = "UNFRNDACT";
		final String GETRESACT = "GETRESACT";
		final String POSTCMTACT = "POSTCMTACT";
		final String DELCMTACT = "DELCMTACT";		
		final String GETCMTACT = "GETCMTACT";
		final String VIEWFEEDACT = "VIEWFEEDACT";
		
		double getprofileactionproportion = Double.parseDouble(p.getProperty(GETRANDOMPROFILEACTION_PROPORTION_PROPERTY,GETRANDOMPROFILEACTION_PROPORTION_PROPERTY_DEFAULT));
		double getfriendsactionproportion = Double.parseDouble(p.getProperty(GETRANDOMLISTOFFRIENDSACTION_PROPORTION_PROPERTY,GETRANDOMLISTOFFRIENDSACTION_PROPORTION_PROPERTY_DEFAULT));
		double getpendingrequestsactionproportion = Double.parseDouble(p.getProperty(GETRANDOMLISTOFPENDINGREQUESTSACTION_PROPORTION_PROPERTY,GETRANDOMLISTOFPENDINGREQUESTSACTION_PROPORTION_PROPERTY_DEFAULT));
		double inviteFriendactionproportion = Double.parseDouble(p.getProperty(INVITEFRIENDSACTION_PROPORTION_PROPERTY,INVITEFRIENDSACTION_PROPORTION_PROPERTY_DEFAULT));
		double acceptfriendactionproportion = Double.parseDouble(p.getProperty(ACCEPTFRIENDSACTION_PROPORTION_PROPERTY,ACCEPTFRIENDSACTION_PROPORTION_PROPERTY_DEFAULT));
		double rejectfriendactionproportion = Double.parseDouble(p.getProperty(REJECTFRIENDSACTION_PROPORTION_PROPERTY,REJECTFRIENDSACTION_PROPORTION_PROPERTY_DEFAULT));
		double unfriendfriendactionproportion = Double.parseDouble(p.getProperty(UNFRIENDFRIENDSACTION_PROPORTION_PROPERTY,UNFRIENDFRIENDSACTION_PROPORTION_PROPERTY_DEFAULT));
		double gettopresourcesactionproportion = Double.parseDouble(p.getProperty(GETTOPRESOURCEACTION_PROPORTION_PROPERTY,GETTOPRESOURCEACTION_PROPORTION_PROPERTY_DEFAULT));
		double postcommentonresourceactionproportion = Double.parseDouble(p.getProperty(POSTCOMMENTONRESOURCEACTION_PROPORTION_PROPERTY,POSTCOMMENTONRESOURCEACTION_PROPORTION_PROPERTY_DEFAULT));
		double delcommentonresourceactionproportion = Double.parseDouble(p.getProperty(DELCOMMENTONRESOURCEACTION_PROPORTION_PROPERTY,DELCOMMENTONRESOURCEACTION_PROPORTION_PROPERTY_DEFAULT));
		double viewcommentonresourceactionproportion = Double.parseDouble(p.getProperty(GETCOMMENTSONRESOURCEACTION_PROPORTION_PROPERTY,GETCOMMENTSONRESOURCEACTION_PROPORTION_PROPERTY_DEFAULT));
		double viewnewsfeedactionproportion = Double.parseDouble(p.getProperty(VIEWNEWSFEEDACTION_PROPORTION_PROPERTY,VIEWNEWSFEEDACTION_PROPORTION_PROPERTY_DEFAULT));

		actions.put(GETPROACT, getprofileactionproportion);
		actions.put(GETFRNDLSTACT, getfriendsactionproportion);
		actions.put(GETPENDACT, getpendingrequestsactionproportion);
		actions.put(INVFRNDACT, inviteFriendactionproportion);
		actions.put(ACCFRNDACT, acceptfriendactionproportion);
		actions.put(REJFRNDACT,rejectfriendactionproportion);
		actions.put(UNFRNDACT, unfriendfriendactionproportion);
		actions.put(GETRESACT, gettopresourcesactionproportion);
		actions.put(POSTCMTACT, postcommentonresourceactionproportion);
		actions.put(DELCMTACT, delcommentonresourceactionproportion);
		actions.put(GETCMTACT, viewcommentonresourceactionproportion);
		actions.put(VIEWFEEDACT, viewnewsfeedactionproportion);
		return actions;
	}

	/**
	 * @param Properties 
	 * return
	 */
	private Map<String, Double> getSessionParameters(Properties p) {
		Map<String,Double> session = new HashMap<String, Double>();
		final String OWNPROFILE = "OWNPROFILE";
		final String FRIENDPROFILE = "FRIENDPROFILE";
		final String POSTCOMMENT = "POSTCOMMENT";
		final String DELCOMMENT = "DELCOMMENT";
		final String ACCEPTREQ = "ACCEPTREQ";
		final String REJECTREQ = "REJECTREQ";
		final String UNFRNDACCEPTREQ = "UNFRNDACCEPTREQ";
		final String UNFRNDREJECTREQ = "UNFRNDREJECTREQ";
		final String UNFRNDREQ = "UNFRNDREQ";
		final String GENFRNDREQ = "GENFRNDREQ";

		double getownprofileproportion=Double.parseDouble(p.getProperty(GETOWNPROFILE_PROPORTION_PROPERTY,GETOWNPROFILE_PROPORTION_PROPERTY_DEFAULT));
		double getfriendprofileproportion=Double.parseDouble(p.getProperty(GETFRIENDPROFILE_PROPORTION_PROPERTY,GETFRIENDPROFILE_PROPORTION_PROPERTY_DEFAULT));
		double postcommentonresourceproportion=Double.parseDouble(p.getProperty(POSTCOMMENTONRESOURCE_PROPORTION_PROPERTY,POSTCOMMENTONRESOURCE_PROPORTION_PROPERTY_DEFAULT));
		double delcommentonresourceproportion=Double.parseDouble(p.getProperty(DELCOMMENTONRESOURCE_PROPORTION_PROPERTY,DELCOMMENTONRESOURCE_PROPORTION_PROPERTY_DEFAULT));
		double acceptfriendshipproportion=Double.parseDouble(p.getProperty(ACCEPTFRIENDSHIP_PROPORTION_PROPERTY,ACCEPTFRIENDSHIP_PROPORTION_PROPERTY_DEFAULT));
		double rejectfriendshipproportion=Double.parseDouble(p.getProperty(REJECTFRIENDSHIP_PROPORTION_PROPERTY,REJECTFRIENDSHIP_PROPORTION_PROPERTY_DEFAULT));
		double unfriendfriendproportion=Double.parseDouble(p.getProperty(UNFRIEND_PROPORTION_PROPERTY,UNFRIEND_PROPORTION_PROPERTY_DEFAULT));
		double unfriendacceptfriendshipproportion=Double.parseDouble(p.getProperty(UNFRIENDACCEPTFRIENDSHIP_PROPORTION_PROPERTY,UNFRIENDACCEPTFRIENDSHIP_PROPORTION_PROPERTY_DEFAULT));
		double unfriendrejectfriendshipproportion=Double.parseDouble(p.getProperty(UNFRIENDREJECTFRIENDSHIP_PROPORTION_PROPERTY,UNFRIENDREJECTFRIENDSHIP_PROPORTION_PROPERTY_DEFAULT));
		double genfriendshipproportion=Double.parseDouble(p.getProperty(GENERATEFRIENDSHIP_PROPORTION_PROPERTY,GENERATEFRIENDSHIP_PROPORTION_PROPERTY_DEFAULT));

		session.put(OWNPROFILE, getownprofileproportion);
		session.put(FRIENDPROFILE, getfriendprofileproportion);
		session.put(POSTCOMMENT, postcommentonresourceproportion);
		session.put(DELCOMMENT, delcommentonresourceproportion);
		session.put(ACCEPTREQ, acceptfriendshipproportion);
		session.put(REJECTREQ, rejectfriendshipproportion);
		session.put(UNFRNDACCEPTREQ,unfriendacceptfriendshipproportion);
		session.put(UNFRNDREJECTREQ,unfriendrejectfriendshipproportion);
		session.put(UNFRNDREQ, unfriendfriendproportion);
		session.put(GENFRNDREQ, genfriendshipproportion);
		return session;
	}
	@Override
	public boolean resetDBInternalStructures(Properties p, int executionMode){
		if(executionMode == BGMainClass.REPEATED && userRelations != null){
			initStats = new HashMap<String, Integer>();
			for(int o=0; o<acceptedFrnds.length; o++){
				initStats.put("ACCEPTFRND-"+myMemberObjs[o].get_uid(),acceptedFrnds[o].size());
			}
			
			for(int o=0; o<pendingFrnds.length; o++){
				initStats.put("PENDFRND-"+myMemberObjs[o].get_uid(),pendingFrnds[o].size());
			}
			
			Set<Integer> keys = postedComments.keySet();
			Iterator<Integer> it = keys.iterator();
			while(it.hasNext()){
				int key = it.next();
				initStats.put("POSTCOMMENT-"+key, postedComments.get(key).size());
			}
		
			
			System.out.println("Internal structures are set up");
			return true;
		}
		
		try {
			rStat.acquire();
			userRelations = new HashMap[usercount];
			for(int i=0; i<myMemberObjs.length; i++){
				//initially adding every user to the related vector of themselves
				HashMap<Integer, String> init = new HashMap<Integer, String>();
				init.put(myMemberObjs[i].get_uid(),"");
				userRelations[i] = init;
			}
			rStat.release();
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
			return false;
		}
		//initialize the pendingFrnds, the acceptedFrnds and the createdresources data structures
		initPendingFriendsAcceptFriendsCreatedResourcesDataStructures(p);
		if(p.getProperty(Client.INIT_STATS_REQ_APPROACH_PROPERTY) == null){
			return true;
		}	
		boolean flag = readInitStatsReqApproachProperty(p);
		return flag;
	}

	/**
	 * @param properties
	 * return
	 */
	private boolean readInitStatsReqApproachProperty(Properties p) {
		boolean flag = true; 
		String queryData = "QUERYDATA";
		String loadFile = "LOADFILE";
		String deterministic = "DETERMINISTIC";
		if(p.getProperty(Client.INIT_STATS_REQ_APPROACH_PROPERTY).equalsIgnoreCase(queryData)){
			flag = initStatsQueryData(p); 
		}
		else if(p.getProperty(Client.INIT_STATS_REQ_APPROACH_PROPERTY).equalsIgnoreCase(loadFile)) {
			flag = initStatsLoadFile(p);
		}
		else if (p.getProperty(Client.INIT_STATS_REQ_APPROACH_PROPERTY).equalsIgnoreCase(deterministic)){
			flag = initStatsDeterministic(p);
		}
		return flag;
	}

	/**
	 * @param Properties 
	 * return
	 */
	private boolean initStatsDeterministic(Properties p) {
		int tUserCount = myMemberObjs.length;
		int numResourcesPerUser = 10;
		int numFriendsPerUser = 100;
		int userOffset = 0;
		int numLoadThread = 1;
		double confPerc = 1;
		if(p.getProperty(Client.RESOURCE_COUNT_PROPERTY) == null || 
				p.getProperty(Client.FRIENDSHIP_COUNT_PROPERTY) == null || 
				p.getProperty(Client.CONFPERC_COUNT_PROPERTY) == null ||
				p.getProperty(Client.NUM_LOAD_THREAD_PROPERTY) == null )
		{
			System.out.println("parameters not specified for initapproach=deterministic. " +
					"Be sure to include resourcecountperuser, friendcountperuser, confperc and numloadthreads )");
			return false;
		}
		
		numResourcesPerUser = Integer.parseInt(p.getProperty(Client.RESOURCE_COUNT_PROPERTY));
		numFriendsPerUser = Integer.parseInt(p.getProperty(Client.FRIENDSHIP_COUNT_PROPERTY));
		confPerc = Double.parseDouble(p.getProperty(Client.CONFPERC_COUNT_PROPERTY));
		userOffset = Integer.parseInt(p.getProperty(Client.USER_OFFSET_PROPERTY));
		numLoadThread = Integer.parseInt(p.getProperty(Client.NUM_LOAD_THREAD_PROPERTY));
		
		if(confPerc != 0 && confPerc != 1.0)
		{
			System.out.println("confperc must be 0 or 1 to use initapproach=deterministic");
			return false;
		}
		else if(userOffset != 0)
		{
			System.out.println("non-zero useroffset not currently supported for initapproach=loadfile");
			return false;
		}
		
		if(Integer.parseInt(p.getProperty(Client.USER_COUNT_PROPERTY, Client.USER_COUNT_PROPERTY_DEFAULT)) <  numLoadThread){
			numLoadThread = 5;
		}
		if(Integer.parseInt(p.getProperty(Client.USER_COUNT_PROPERTY, Client.USER_COUNT_PROPERTY_DEFAULT)) % numLoadThread != 0){
			while(Integer.parseInt(p.getProperty(Client.USER_COUNT_PROPERTY, Client.USER_COUNT_PROPERTY_DEFAULT))% numLoadThread != 0)
				numLoadThread--; 
		}
		//ensure the friendship creation within clusters for each thread makes sense
		if(numFriendsPerUser != 0){
			int tmp = Integer.parseInt(p.getProperty(Client.USER_COUNT_PROPERTY, Client.USER_COUNT_PROPERTY_DEFAULT))/numLoadThread;
			while(tmp <= numFriendsPerUser){
				numLoadThread--;
				while(Integer.parseInt(p.getProperty(Client.USER_COUNT_PROPERTY, Client.USER_COUNT_PROPERTY_DEFAULT))%numLoadThread != 0)
					numLoadThread--;
				tmp = Integer.parseInt(p.getProperty(Client.USER_COUNT_PROPERTY, Client.USER_COUNT_PROPERTY_DEFAULT))/numLoadThread;					
			} 
		}
		//verify fragment size related to friendships
		if(tUserCount < numFriendsPerUser+1){
			System.out.println("Fragment size is too small, can't determine appropriate friendships. exiting.");
			return false;
		}
		//verify load thread count and fragment size
		if(numFriendsPerUser > tUserCount/numLoadThread){
			System.out.println("Could not have loaded with "+numLoadThread+" so considering 1 thread as number of load threads for deterministic");
			numLoadThread = 1;
		} 
		final String PENDFRND = "PENDFRND-";
		final String ACCEPTFRND = "ACCEPTFRND-";
		final String POSTCOMMENT = "POSTCOMMENT-";
		
		for( int i = 0; i < tUserCount; i++ )
		{
			int memberUID = myMemberObjs[i].get_uid();
			int numResourcesperMember = memberUID * numResourcesPerUser;
			if(confPerc == 1){
				initStats.put(PENDFRND+memberUID, 0);
				initStats.put(ACCEPTFRND+memberUID, numFriendsPerUser);
			}else{
				initStats.put(PENDFRND+memberUID, numFriendsPerUser/2);
				initStats.put(ACCEPTFRND+memberUID, 0);
			}
			//find the resources created for each user
			for(int j = 0; j < numResourcesPerUser; j++)
			{
				initStats.put(POSTCOMMENT+(numResourcesperMember +j), 0);
				createdResources[i].add(numResourcesperMember +j);
			}
			
		}
		
		//create the actual friendship or pending relationships
		createFriendshipPendingFriendship(tUserCount, numFriendsPerUser, numLoadThread, confPerc);
return true;
	}

	/**
	 * @param tUserCount
	 * @param numFriendsPerUser
	 * @param numLoadThread
	 * @param confPerc
	 */
	private void createFriendshipPendingFriendship(int tUserCount,
			int numFriendsPerUser, int numLoadThread, double confPerc) {
		int numUsersPerLoadThread = tUserCount/numLoadThread;
		int remaining = tUserCount - (numUsersPerLoadThread*numLoadThread);
		int addUserCnt = 0;
		for(int i = 0; i < numLoadThread; i++)
		{
			if(i == numLoadThread -1)
				addUserCnt = remaining;
			for(int u = i*numUsersPerLoadThread; u<numUsersPerLoadThread+i*numUsersPerLoadThread+addUserCnt; u++){
				int uidx = u;
				int ukey = myMemberObjs[uidx].get_uid();
				for(int j=0; j<numFriendsPerUser/2; j++){
					int fidx = i*numUsersPerLoadThread+((u-i*numUsersPerLoadThread)+j+1)%(numUsersPerLoadThread+addUserCnt);
					int fkey = myMemberObjs[fidx].get_uid();
					relateUsers(ukey, fkey);
					if(confPerc == 1){
						acceptedFrnds[uidx].put(fkey, null);
						acceptedFrnds[fidx].put(ukey, null);
					}else{
						pendingFrnds[fidx].add(ukey);
						if(enforceFriendship && usersWithpendingFrnds.get(ukey) == null){
							usersWithpendingFrnds.put(ukey, numFriendsPerUser/2);
						}
					}
				}
			}
		}
	}

	/**
	 * @param Properties 
	 * return
	 */
	private boolean initStatsLoadFile(Properties p) {
		int tUserCount = myMemberObjs.length;
		int numResourcesPerUser = 10;
		int numFriendsPerUser = 100;
		int userOffset = 0;
		double confPerc = 1;
		
		int friendId;
		
		if(p.getProperty(Client.RESOURCE_COUNT_PROPERTY) == null || 
				p.getProperty(Client.FRIENDSHIP_COUNT_PROPERTY) == null || 
				p.getProperty(Client.CONFPERC_COUNT_PROPERTY) == null || 
				p.getProperty(Client.USER_OFFSET_PROPERTY) == null)
		{
			System.out.println("Parameters not specified for initapproach=loadfile. " +
					"Be sure to include the file used to load the data (e.g: -P workloads/populateDB)");
			return false;
		}
		
		numResourcesPerUser = Integer.parseInt(p.getProperty(Client.RESOURCE_COUNT_PROPERTY));
		numFriendsPerUser = Integer.parseInt(p.getProperty(Client.FRIENDSHIP_COUNT_PROPERTY));
		confPerc = Double.parseDouble(p.getProperty(Client.CONFPERC_COUNT_PROPERTY));
		userOffset = Integer.parseInt(p.getProperty(Client.USER_OFFSET_PROPERTY));
		
		if(confPerc != 0 && confPerc != 1.0)
		{
			System.out.println("confperc must be 0 or 1 to use initapproach=loadfile");
			return false;
		}
		else if(userOffset != 0)
		{
			System.out.println("non-zero useroffset not currently supported for initapproach=loadfile");
			return false;
		}
		
		
		for( int i = 0; i < tUserCount; i++ )
		{
			initStats.put("PENDFRND-"+i, 0);
			initStats.put("ACCEPTFRND-"+i, numFriendsPerUser);
			for(int j = 0; j < numResourcesPerUser; j++)
			{
				initStats.put("POSTCOMMENT-"+(i*numResourcesPerUser +j), 0);
			}
			
			for(int j = 0; j < numFriendsPerUser + 1; j++)
			{
				friendId = i - (numFriendsPerUser / 2) + j;
				if(friendId < 0)
				{
					friendId += tUserCount;
				}
				else if(friendId >= tUserCount)
				{
					friendId -= tUserCount;
				}
				
				if(friendId != i)
				{
					acceptedFrnds[i].put(friendId, null);
					relateUsers(i, friendId);
				}
			}
		}
		return true;
	}

	/**
	 * @param Properties 
	 * return
	 */
	private boolean initStatsQueryData(Properties p) {
		int numQThreads = 5;
		Vector<initQueryThread> qThreads = new Vector<initQueryThread>();
		int tUserCount = myMemberObjs.length / numQThreads;
		int remainingUsers = myMemberObjs.length - (tUserCount*numQThreads);
		int addUser = 0;
		for(int u=0; u<numQThreads; u++){
			if(u == numQThreads-1)
				addUser = remainingUsers;
			int[] tMembers = new int[tUserCount+addUser];
			for(int d=0; d<tUserCount+addUser; d++)
				tMembers[d] = myMemberObjs[d+u*tUserCount].get_uid();
			initQueryThread t = new initQueryThread(tMembers, p);
			qThreads.add(t);
			t.start();	
		}
		for (Thread t : qThreads) {
			try {
				t.join();
				initStats.putAll(((initQueryThread)t).getInit());
				HashMap<Integer, Vector<Integer>> pends = ((initQueryThread)t).getPendings();
				Set<Integer> keys = pends.keySet();
				Iterator<Integer> it = keys.iterator();
				while(it.hasNext()){
					int aKey = (it.next());
					pendingFrnds[memberIdxs.get(aKey)] = pends.get(aKey);
					//needed for enforced friendship
					if(enforceFriendship && pends.get(aKey).size() > 0)
							usersWithpendingFrnds.put(aKey,pends.get(aKey).size());
					for(int d=0; d<pends.get(aKey).size(); d++){
						relateUsers(aKey, pends.get(aKey).get(d));
					}
				}
				
				HashMap<Integer, Vector<Integer>> confs = ((initQueryThread)t).getConfirmed();
				keys = confs.keySet();
				it = keys.iterator();
				while(it.hasNext()){
					int aKey = (it.next());
					for(int d=0; d<confs.get(aKey).size(); d++){
						acceptedFrnds[memberIdxs.get(aKey)].put(confs.get(aKey).get(d),null);
						relateUsers(aKey, confs.get(aKey).get(d));
					}
				}
				
				HashMap<Integer, Vector<Integer>> resources = ((initQueryThread)t).getResources();
				Set<Integer> ukeys = resources.keySet();
				it = ukeys.iterator();
				while(it.hasNext()){
					int uKey = (it.next());
					createdResources[memberIdxs.get(uKey)] = resources.get(uKey);
				}
				((initQueryThread)t).freeResources();
			}catch(Exception e){
				e.printStackTrace(System.out);
				return false;
			}
		}
		
		qThreads.clear();
		qThreads = null;

		return true;
	}

	/**
	 * @param Properties 
	 */
	private void initPendingFriendsAcceptFriendsCreatedResourcesDataStructures(
			Properties p) {
		int numResPerUser = Integer.parseInt(p.getProperty(Client.RESOURCE_COUNT_PROPERTY, Client.RESOURCE_COUNT_PROPERTY_DEFAULT));
		usersWithpendingFrnds = new HashMap<Integer, Integer>();
		pendingFrnds = new Vector[usercount];
		acceptedFrnds = new HashMap[usercount];
		createdResources = new Vector[usercount];
		postedComments = new HashMap<Integer, Vector<Integer>>();
		maxCommentIds = new HashMap<Integer, Integer>(0);
		for(int i=0; i<myMemberObjs.length;i++){
			pendingFrnds[i]= new Vector<Integer>();
			acceptedFrnds[i] = new HashMap<Integer,String>();
			createdResources[i] = new Vector<Integer>();
			for(int j=0; j<numResPerUser; j++){
				postedComments.put((myMemberObjs[i].get_uid()*numResPerUser+j), new Vector<Integer>());
				maxCommentIds.put((myMemberObjs[i].get_uid()*numResPerUser+j), 0);
			}
		}
	}

	/**
	 * Do one transaction operation. Because it will be called concurrently from multiple client threads, this 
	 * function must be thread safe. 
	 * returns the number of actions done
	 */
	public int doTransaction(DB db, Object threadstate, int threadid,  StringBuilder updateLog, StringBuilder readLog,  int seqID, HashMap<String, Integer> resUpdateOperations
			, HashMap<String, Integer> friendshipInfo, HashMap<String, Integer> pendingInfo, int thinkTime, boolean insertImage, boolean warmup) {
		String op=operationchooser.nextString();
		int opsDone = 0;
		
		//actions
		if (op.compareTo("GETPROACT") == 0){
			opsDone = doActionGetProfile(db, threadid, updateLog, readLog ,seqID, insertImage, warmup);
		}
		else if (op.compareTo("GETFRNDLSTACT") == 0)
		{
			opsDone = doActionGetFriends(db,threadid, updateLog,readLog,seqID, insertImage, warmup);
		}
		else if (op.compareTo("GETPENDACT") == 0)
		{
			opsDone = doActionGetPendings(db,threadid, updateLog,readLog,seqID, insertImage, warmup);
		}
		else if (op.compareTo("INVFRNDACT") == 0)
		{
			opsDone = doActioninviteFriend(db,threadid, updateLog,readLog, seqID, friendshipInfo, pendingInfo, insertImage, warmup);
		}
		else if (op.compareTo("ACCFRNDACT") == 0)
		{
			opsDone = doActionAcceptFriends(db, threadid, updateLog,readLog, seqID, friendshipInfo,pendingInfo, thinkTime,  insertImage,warmup);
		}
		else if (op.compareTo("REJFRNDACT") == 0)
		{
			opsDone = doActionRejectFriends(db, threadid, updateLog,readLog, seqID, friendshipInfo,pendingInfo, thinkTime,  insertImage,warmup);
		}
		else if (op.compareTo("UNFRNDACT") == 0)
		{
			opsDone = doActionUnFriendFriends(db, threadid,updateLog,readLog, seqID, friendshipInfo
					,pendingInfo, thinkTime,  insertImage, warmup);
		}
		else if (op.compareTo("GETRESACT") == 0)
		{
			opsDone = doActionGetTopResources(db, threadid, updateLog, readLog ,seqID, insertImage,  warmup);
		}
		else if (op.compareTo("GETCMTACT") == 0)
		{
			opsDone = doActionviewCommentOnResource(db, threadid, updateLog, readLog ,seqID, thinkTime, insertImage,  warmup);
		}
		else if (op.compareTo("POSTCMTACT") == 0)
		{
			opsDone = doActionPostComments(db,threadid, updateLog, readLog,seqID, resUpdateOperations,thinkTime, insertImage,  warmup);
		}
		else if (op.compareTo("DELCMTACT") == 0)
		{
			opsDone = doActionDelComments(db,threadid, updateLog, readLog,seqID, resUpdateOperations,thinkTime, insertImage,  warmup);
		}
		//session
		else {
			if (op.compareTo("OWNPROFILE")==0)
			{
				opsDone = doTransactionOwnProfile(db, threadid, updateLog, readLog ,seqID, thinkTime, insertImage,  warmup);
			}
			else if (op.compareTo("FRIENDPROFILE")==0)
			{
				opsDone = doTransactionFriendProfile(db, threadid, updateLog, readLog,seqID, thinkTime,  insertImage, warmup);
			}
			else if (op.compareTo("POSTCOMMENT")==0)
			{
				opsDone = doTransactionPostCommentOnResource(db,threadid, updateLog, readLog,seqID, resUpdateOperations, thinkTime, insertImage, warmup);
			}
			else if (op.compareTo("DELCOMMENT")==0)
			{
				opsDone = doTransactionDeleteCommentOnResource(db,threadid, updateLog, readLog,seqID, resUpdateOperations, thinkTime, insertImage, warmup);
			}
			else if (op.compareTo("ACCEPTREQ")==0)
			{
				opsDone = doTransactionAcceptFriendship(db,threadid,  updateLog, readLog,seqID, friendshipInfo, pendingInfo, thinkTime, insertImage, warmup);
			}
			else if (op.compareTo("REJECTREQ") == 0)
			{
				opsDone = doTransactionRejectFriendship(db,threadid,  updateLog,readLog ,seqID, friendshipInfo, pendingInfo, thinkTime, insertImage, warmup);
			}
			else if (op.compareTo("UNFRNDACCEPTREQ") == 0)
			{
				opsDone = doTransactionUnfriendPendingFriendship(db,threadid, updateLog,readLog, seqID, friendshipInfo, pendingInfo, thinkTime, "ACCEPT", insertImage, warmup);
			}
			else if (op.compareTo("UNFRNDREJECTREQ") == 0)
			{
				opsDone = doTransactionUnfriendPendingFriendship(db,threadid, updateLog, readLog,seqID, friendshipInfo, pendingInfo, thinkTime, "REJECT", insertImage, warmup);
			}
			else if (op.compareTo("UNFRNDREQ") == 0)
			{
				opsDone = doTransactionUnfriendFriendship(db,threadid,  updateLog, readLog,seqID, friendshipInfo, pendingInfo, thinkTime, insertImage, warmup);
			}
			else if (op.compareTo("GENFRNDREQ") == 0)
			{
				opsDone = doTransactionGenerateFriendship(db,threadid,  updateLog, readLog ,seqID, friendshipInfo, pendingInfo, thinkTime, insertImage, warmup);
			}
		}
		return opsDone;
	}

	private int activateKeynameAndUpdateFreq() {
		int keyname = buildKeyName();
		//activate the user so no one else can grab it
		keyname = activateUser(keyname);
		if(keyname == -1) {
			return keyname;
		}
		//update frequency of access for the picked user
		incrUserRef(keyname);
		return keyname;
	}
	
	private int activateKeynameAndUpdateFreqWrite() {
		int keyname = buildKeyNameWrite();
		//activate the user so no one else can grab it
		keyname = activateUser(keyname);
		if(keyname == -1) {
			return keyname;
		}
		//update frequency of access for the picked user
		incrUserRef(keyname);
		return keyname;
	}	
	

	/**
	 * @param db
	 * @param threadid
	 * @param readLog
	 * @param seqID
	 * @param insertImage
	 * @param warmup
	 * @param keyname
	 * return
	 */
	private int viewProfileTransaction(DB db, int threadid,
			StringBuilder readLog, int seqID, boolean insertImage,
			boolean warmup, int anotherUser, int keyname) {
		String actionType = "ViewProfile";
		int numOpsDone =0;
		HashMap<String,ByteIterator> pResult=new HashMap<String,ByteIterator>();
		long startReadp = System.nanoTime();
		int ret = db.viewProfile(anotherUser, keyname, pResult, insertImage, false);
		if(ret < 0){
			System.out.println("There is an exception in getProfile.");
			System.exit(0);
		}	
		long endReadp = System.nanoTime();
		numOpsDone++;
		if(!warmup && enableLogging){
			readLog.append("READ,ACCEPTFRND,"+seqID+","+threadid+","+keyname+","+startReadp+","+endReadp+","+pResult.get("friendcount")+ "," + actionType +"\n");
			readLog.append("READ,PENDFRND,"+seqID+","+threadid+","+keyname+","+startReadp+","+endReadp+","+pResult.get("pendingcount")+ "," + actionType +"\n");
			readsExist = true;
		}
		return numOpsDone;
	}

	/**
	 * @param db
	 * @param keyname
	 * @param numOpsDone
	 * return
	 */
	private int viewTopKResourceTransaction(DB db, int anotherUser, int keyname) {
		int ret, numOpsDone = 0;
		Vector<HashMap<String,ByteIterator>> rResult=new Vector<HashMap<String,ByteIterator>>();		
		ret = db.viewTopKResources(anotherUser, keyname, 5, rResult);
		if(ret < 0){
			System.out.println("There is an exception in getTopResource.");
			System.exit(0);
		}
		numOpsDone++;
		return numOpsDone;
	}
	
	public int buildKeyName() {
		int key =0;
		if(requestdistrib.compareTo("dzipfian")==0){
			int idx = myDist.GenerateOneItem()-1;
			key = myMemberObjs[idx].get_uid();
		}else
			key = keychooser.nextInt()+useroffset;

		return key;
	}
	
	public int buildKeyNameWrite() {
		int key =0;
		if(requestdistrib.compareTo("dzipfian")==0){
			int idx = myDist2.GenerateOneItem()-1+clusterOffset;
			key = myMemberObjs[idx].get_uid();
			//System.out.println("Write Operation Happened key="+key+"idx="+idx);
		}else
			key = keychooser.nextInt()+useroffset;
		
		return key;
	}


	public int doTransactionOwnProfile(DB db, int threadid, StringBuilder updateLog, StringBuilder readLog,int seqID, int thinkTime, boolean insertImage,  boolean warmup)
	{	
		int numOpsDone = 0; 
		int keyname = activateKeynameAndUpdateFreq();
		if(keyname == -1) {
			return 0;
		}
		
		numOpsDone += viewProfileTransaction(db, threadid, readLog, seqID, insertImage, warmup, keyname, keyname);
		numOpsDone += viewTopKResourceTransaction(db, keyname, keyname);
		
		deactivateUser(keyname);
		return numOpsDone;
	}

	

	public int doTransactionFriendProfile(DB db, int threadid, StringBuilder updateLog,StringBuilder readLog, int seqID, int thinkTime,  boolean insertImage, boolean warmup)
	{
		String actionType = "FriendsProfile";
		int numOpsDone = 0; 
		int keyname = activateKeynameAndUpdateFreq();
		if(keyname == -1) {
			return 0;
		}
		
		numOpsDone += viewProfileTransaction(db, threadid, readLog, seqID, insertImage, warmup, keyname, keyname);
		numOpsDone += viewTopKResourceTransaction(db, keyname, keyname);
		
		try {
			Thread.sleep(thinkTime);
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}
		
		Vector<HashMap<String,ByteIterator>> fResult=new Vector<HashMap<String,ByteIterator>>();
		long startReadf = System.nanoTime();
		int ret = db.listFriends(keyname, keyname, null, fResult,  insertImage, false);
		if(ret < 0){
			System.out.println("There is an exception in listFriends.");
			System.exit(0);
		}
		long endReadf = System.nanoTime();
		numOpsDone++;
		if(!warmup && enableLogging){
			readLog.append("READ,ACCEPTFRND,"+seqID+","+threadid+","+keyname+","+startReadf+","+endReadf+","+fResult.size() + ","+actionType+"\n");
			readsExist = true;
		}
		
		try {
			Thread.sleep(thinkTime);
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}
		
		if(fResult.size() > 0){
			Random random = new Random();
			int idx = random.nextInt(fResult.size());
			HashMap<String,ByteIterator> fpResult=new HashMap<String,ByteIterator>();
			int friendId = -1;
			friendId = Integer.parseInt(fResult.get(idx).get("userid").toString());
			startReadf = System.nanoTime();
			ret = db.viewProfile(keyname, friendId, fpResult, insertImage, false);
			if(ret < 0){
				System.out.println("There is an exception in getProfile.");
				System.exit(0);
			}
			endReadf = System.nanoTime();
			numOpsDone++;
			if(!warmup && enableLogging){
				readLog.append("READ,ACCEPTFRND,"+seqID+","+threadid+","+friendId+","+startReadf+","+endReadf+","+fpResult.get("friendcount")+ "," + actionType +"\n");
				//this if should never be true
				if(keyname == friendId){
					readLog.append("READ,PENDFRND,"+seqID+","+threadid+","+friendId+","+startReadf+","+endReadf+","+fpResult.get("pendingcount")+ "," + actionType +"\n");
				}
				readsExist = true;
			}
			
			try {
				Thread.sleep(thinkTime);
			} catch (InterruptedException e) {
				e.printStackTrace(System.out);
			}
			
			Vector<HashMap<String, ByteIterator>> rResult = new Vector<HashMap<String,ByteIterator>>();		
			ret = db.viewTopKResources(keyname, friendId, 5, rResult);
			if(ret < 0){
				System.out.println("There is an exception in getTopResource.");
				System.exit(0);
			}
			numOpsDone++;
		}	

		deactivateUser(keyname);
		return numOpsDone;
	}

	public int doTransactionPostCommentOnResource(DB db,int threadid, StringBuilder updateLog, StringBuilder readLog,int seqID, HashMap<String, Integer> resUpdateOperations, int thinkTime, boolean insertImage,  boolean warmup)
	{	
		int numOpsDone=0;
		int commentor = activateKeynameAndUpdateFreqWrite();
		if(commentor == -1) {
			return 0;
		}
		
		numOpsDone += viewProfileTransaction(db, threadid, readLog, seqID, insertImage, warmup, commentor, commentor);
		numOpsDone += viewTopKResourceTransaction(db, commentor, commentor);
		
		try {
			Thread.sleep(thinkTime);
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}
		
		int keyname = buildKeyNameWrite();
		
		numOpsDone += viewProfileTransaction(db, threadid, readLog, seqID, insertImage, warmup, commentor, keyname);
		int ret;
		Vector<HashMap<String, ByteIterator>> rResult = new Vector<HashMap<String,ByteIterator>>();
		ret = db.viewTopKResources(commentor, keyname, 5, rResult);
		if(ret < 0){
			System.out.println("There is an exception in getTopResource.");
			System.exit(0);
		}
		numOpsDone++;
		
		try {
			Thread.sleep(thinkTime);
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}
		
		if(rResult.size() > 0){
			Random random = new Random();
			int idx = random.nextInt(rResult.size());
			String resourceID = "";
			String ownerID ="";
			resourceID = rResult.get(idx).get("rid").toString();
			ownerID= rResult.get(idx).get("creatorid").toString();
			
			numOpsDone += viewCommentOnResourceTransaction(db, threadid, readLog,
					seqID, warmup, commentor, keyname, resourceID);

			try {
				Thread.sleep(thinkTime);
			} catch (InterruptedException e) {
				e.printStackTrace(System.out);
			}
			
			if(!warmup){
				HashMap<String,ByteIterator> commentValues = new HashMap<String, ByteIterator>();
				createCommentAttrs(commentValues);
				try {
					sCmts.acquire();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				
				numOpsDone += postCommentTransaction(db, threadid, updateLog,
						seqID, resUpdateOperations, commentor,
						resourceID, ownerID, commentValues);
				
				try {
					Thread.sleep(thinkTime);
				} catch (InterruptedException e) {
					e.printStackTrace(System.out);
				}
			}
			
			numOpsDone += viewCommentOnResourceTransaction(db, threadid, readLog, seqID,
					warmup, commentor, keyname, resourceID);
		}	
		deactivateUser(commentor);
		return numOpsDone;
	}

	/**
	 * @param db
	 * @param threadid
	 * @param readLog
	 * @param seqID
	 * @param warmup
	 * @param commentor
	 * @param keyname
	 * @param resourceID
	 */
	private int viewCommentOnResourceTransaction(DB db, int threadid, StringBuilder readLog, int seqID, boolean warmup, int commentor, int keyname, String resourceID) {
		String actionType = "ViewCommentOnResource";
		int ret;
		Vector<HashMap<String, ByteIterator>> cResult;
		int numOpsDone = 0;
		long startRead2 = System.nanoTime();
		cResult=new Vector<HashMap<String,ByteIterator>>();
		ret = db.viewCommentOnResource(commentor, keyname, Integer.parseInt(resourceID), cResult);
		if(ret < 0){
			System.out.println("There is an exception in viewCommentOnResource.");
			System.exit(0);
		}
		long endRead2 = System.nanoTime();
		numOpsDone++;
		if(!warmup && enableLogging){
			readLog.append("READ,POSTCOMMENT,"+seqID+","+threadid+","+resourceID+","+startRead2+","+endRead2+","+cResult.size() + "," + actionType + "\n");
			readsExist = true;
		}
		return numOpsDone;
	}

	/**
	 * @param db
	 * @param threadid
	 * @param updateLog
	 * @param seqID
	 * @param resUpdateOperations
	 * @param numOpsDone
	 * @param commentor
	 * @param resourceID
	 * @param ownerID
	 * @param commentValues
	 * return
	 */
	private int postCommentTransaction(DB db, int threadid,
			StringBuilder updateLog, int seqID,
			HashMap<String, Integer> resUpdateOperations,
			int commentor, String resourceID, String ownerID,
			HashMap<String, ByteIterator> commentValues) {
		String actionType = "PostComment";
		int ret, numOpsDone = 0;
		int mid = maxCommentIds.get(Integer.parseInt(resourceID))+1;
		maxCommentIds.put(Integer.parseInt(resourceID), mid);
		sCmts.release();
		commentValues.put("mid", new ObjectByteIterator(Integer.toString(mid).getBytes()));
		long startUpdate = System.nanoTime();
		ret =db.postCommentOnResource(commentor, Integer.parseInt(ownerID), Integer.parseInt(resourceID), commentValues);
		if(ret < 0){
			System.out.println("There is an exception in postComment.");
			System.exit(0);
		}
		long endUpdate = System.nanoTime();
		//if I add it before the update , a delete may delete it without it actually being in the database
		//resulting in wrong results
		postedComments.get(Integer.parseInt(resourceID)).add(mid);	
		numOpsDone++;
		int numUpdatesTillNow = 0;
		if(resUpdateOperations.get(resourceID)!= null){
			numUpdatesTillNow = resUpdateOperations.get(resourceID);
		}
		resUpdateOperations.put(resourceID, (numUpdatesTillNow+1));
		if(enableLogging){
			updateLog.append("UPDATE,POSTCOMMENT,"+seqID+","+threadid+","+resourceID+","+startUpdate+","+endUpdate+","+(numUpdatesTillNow+1)+",I"+ "," + actionType +"\n");
			updatesExist = true;
		}
		return numOpsDone;
	}

	
	public int doTransactionDeleteCommentOnResource(DB db,int threadid, StringBuilder updateLog, StringBuilder readLog,int seqID, HashMap<String, Integer> resUpdateOperations, int thinkTime, boolean insertImage,  boolean warmup)
	{	//deletes a comment posted on a resource on your wall
		
		int numOpsDone = 0; 
		int keyname = activateKeynameAndUpdateFreqWrite();
		if(keyname == -1) {
			return 0;
		}
		
		numOpsDone += viewProfileTransaction(db, threadid, readLog, seqID, insertImage, warmup, keyname, keyname);
		Vector<HashMap<String,ByteIterator>> rResult=new Vector<HashMap<String,ByteIterator>>();
		int ret = db.viewTopKResources(keyname, keyname, 5, rResult);
		if(ret < 0){
			System.out.println("There is an exception in getTopResource.");
			System.exit(0);
		}
		numOpsDone++;
		
		try {
			Thread.sleep(thinkTime);
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}
		
		if(rResult.size() > 0){
			Random random = new Random();
			int idx = random.nextInt(rResult.size());
			
			Vector<HashMap<String,ByteIterator>> cResult=new Vector<HashMap<String,ByteIterator>>();
			String resourceID = "";
			String ownerID ="";
			resourceID = rResult.get(idx).get("rid").toString();
			ownerID= rResult.get(idx).get("creatorid").toString();
			
			numOpsDone += viewCommentOnResourceTransaction(db, threadid, readLog, seqID, warmup, keyname, keyname, resourceID);
			
			try {
				Thread.sleep(thinkTime);
			} catch (InterruptedException e) {
				e.printStackTrace(System.out);
			}
			//if( cResult.size()>0){
			try {
				sCmts.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(postedComments.get(Integer.parseInt(resourceID)) != null && postedComments.get(Integer.parseInt(resourceID)).size()>0 ){
				if(!warmup){
					
					numOpsDone += delCommentOnResourceTransaction(db, threadid,
							updateLog, seqID, resUpdateOperations,
							random, resourceID, ownerID);
					
					try {
						Thread.sleep(thinkTime);
					} catch (InterruptedException e) {
						e.printStackTrace(System.out);
					}
					
				}else {
					sCmts.release();
				}
				
				numOpsDone += viewCommentOnResourceTransaction(db, threadid, readLog, seqID, warmup, keyname, keyname, resourceID);
			}else
				sCmts.release();
		}	
		deactivateUser(keyname);
		return numOpsDone;
	}

	/**
	 * @param db
	 * @param threadid
	 * @param updateLog
	 * @param seqID
	 * @param resUpdateOperations
	 * @param numOpsDone
	 * @param random
	 * @param resourceID
	 * @param ownerID
	 * return
	 */
	private int delCommentOnResourceTransaction(DB db, int threadid,
			StringBuilder updateLog, int seqID,
			HashMap<String, Integer> resUpdateOperations,
			Random random, String resourceID, String ownerID) {
		String actionType = "DeleteCommentonResource";
		int ret, numOpsDone = 0;
		int midx = random.nextInt(postedComments.get(Integer.parseInt(resourceID)).size());
		int mid = postedComments.get(Integer.parseInt(resourceID)).get(midx);
		postedComments.get(Integer.parseInt(resourceID)).remove(midx);
		sCmts.release();
		long startUpdate = System.nanoTime();
		ret = db.delCommentOnResource(Integer.parseInt(ownerID), Integer.parseInt(resourceID), mid);
		if(ret < 0){
			System.out.println("There is an exception in delComment.");
			System.exit(0);
		}
		long endUpdate = System.nanoTime();
		numOpsDone++;
		int numUpdatesTillNow = 0;
		if(resUpdateOperations.get(resourceID)!= null){
			numUpdatesTillNow = resUpdateOperations.get(resourceID);
		}
		resUpdateOperations.put(resourceID, (numUpdatesTillNow-1));
		if(enableLogging){
			updateLog.append("UPDATE,POSTCOMMENT,"+seqID+","+threadid+","+resourceID+","+startUpdate+","+endUpdate+","+(numUpdatesTillNow+1)+",D"+ "," + actionType +"\n");
			updatesExist = true;
		}
		return numOpsDone;
	}


	public int doTransactionGenerateFriendship(DB db, int threadid, StringBuilder updateLog, StringBuilder readLog, int seqID, HashMap<String, Integer> friendshipInfo
			,HashMap<String, Integer> pendingInfo, int thinkTime,  boolean insertImage, boolean warmup)
	{
		int numOpsDone = 0; 
		int keyname = activateKeynameAndUpdateFreqWrite();
		if(keyname == -1) {
			return 0;
		}
		
		numOpsDone += viewProfileTransaction(db, threadid, readLog, seqID, insertImage, warmup, keyname, keyname);
		numOpsDone += viewTopKResourceTransaction(db, keyname, keyname);
		
		try {
			Thread.sleep(thinkTime);
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}
		
		numOpsDone += listFriendsTransaction(db, threadid, readLog, seqID, insertImage, warmup, keyname, keyname);
		
		try {
			Thread.sleep(thinkTime);
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}
		
		int noRelId = -1; 
		noRelId = viewNotRelatedUsers(keyname);
		//if A invited B, B should not be able to invite A else with reject and invite again you will have integrity constraints
		if(noRelId!= -1 && isActive(noRelId) == -1){
			deactivateUser(keyname);
			return numOpsDone;
		}
		if(!warmup){ //so two people would not invite each other at the same time
			if(noRelId == -1){
				//do nothing
			}else{
				numOpsDone += inviteFriendTransaction(db, threadid, updateLog,
						seqID, pendingInfo, keyname, noRelId);
				
				try {
					Thread.sleep(thinkTime);
				} catch (InterruptedException e) {
					e.printStackTrace(System.out);
				}
				
				numOpsDone += viewPendingRequestTransaction(db, threadid,
						readLog, seqID, insertImage, warmup, keyname);
			}	
		}
		deactivateUser(keyname);
		return numOpsDone;
	}

	/**
	 * @param db
	 * @param threadid
	 * @param readLog
	 * @param seqID
	 * @param insertImage
	 * @param warmup
	 * @param numOpsDone
	 * @param keyname
	 * return
	 */
	private int viewPendingRequestTransaction(DB db, int threadid,
			StringBuilder readLog, int seqID, boolean insertImage,
			boolean warmup, int keyname) {
		String actionType = "ViewPendingRequests";
		int ret, numOpsDone = 0;
		Vector<HashMap<String,ByteIterator>> peResult=new Vector<HashMap<String,ByteIterator>>();
		long startReadp = System.nanoTime();
		ret = db.viewFriendReq(keyname, peResult, insertImage, false);
		if(ret < 0){
			System.out.println("There is an exception in viewPendingFriends.");
			System.exit(0);
		}
		long endReadp = System.nanoTime();
		numOpsDone++;
		if(!warmup && enableLogging){
			readLog.append("READ,PENDFRND,"+seqID+","+threadid+","+keyname+","+startReadp+","+endReadp+","+peResult.size()+ "," + actionType +"\n");
			readsExist = true;
		}
		return numOpsDone;
	}

	/**
	 * @param db
	 * @param threadid
	 * @param updateLog
	 * @param seqID
	 * @param pendingInfo
	 * @param numOpsDone
	 * @param keyname
	 * @param noRelId
	 * return
	 */
	private int inviteFriendTransaction(DB db, int threadid,
			StringBuilder updateLog, int seqID,
			HashMap<String, Integer> pendingInfo, int keyname,
			int noRelId) {
		String actionType = "InviteFriend";
		int ret, numOpsDone = 0;
		long startUpdatei = System.nanoTime();
		ret = db.inviteFriend(keyname, noRelId);
		if(ret < 0){
			System.out.println("There is an exception in invFriends.");
			System.exit(0);
		}
		
		pendingFrnds[memberIdxs.get(noRelId)].add(keyname);
		int numPendingsForOtherUserTillNow = 0;
		if(pendingInfo.get(Integer.toString(noRelId))!= null){
			numPendingsForOtherUserTillNow = pendingInfo.get(Integer.toString(noRelId));
		}
		pendingInfo.put(Integer.toString(noRelId), (numPendingsForOtherUserTillNow+1));
		long endUpdatei = System.nanoTime();
		numOpsDone++;
		if(enableLogging){
			updateLog.append("UPDATE,PENDFRND,"+seqID+","+threadid+","+noRelId+","+startUpdatei+","+endUpdatei+","+(numPendingsForOtherUserTillNow+1)+",I"+ "," + actionType  +"\n");
			updatesExist = true;
		}
		relateUsers(keyname, noRelId );
		deactivateUser(noRelId);
		return numOpsDone;
	}

	/**
	 * @param db
	 * @param threadid
	 * @param readLog
	 * @param seqID
	 * @param insertImage
	 * @param warmup
	 * @param numOpsDone
	 * @param keyname
	 * return
	 */
	private int listFriendsTransaction(DB db, int threadid,
			StringBuilder readLog, int seqID, boolean insertImage,
			boolean warmup, int anotherUser, int keyname) {
		String actionType = "ListFriends";
		int numOpsDone = 0;
		Vector<HashMap<String,ByteIterator>> fResult=new Vector<HashMap<String,ByteIterator>>();
		long startReadf = System.nanoTime();
		int ret = db.listFriends(anotherUser, keyname, null, fResult,  insertImage, false);
		if(ret < 0){
			System.out.println("There is an exception in listFriends.");
			System.exit(0);
		}
		long endReadf = System.nanoTime();
		numOpsDone++;
		if(!warmup && enableLogging){
			readLog.append("READ,ACCEPTFRND,"+seqID+","+threadid+","+keyname+","+startReadf+","+endReadf+","+fResult.size()+ "," + actionType +"\n");
			readsExist = true;
		}
		return numOpsDone;
	}

	public int doTransactionAcceptFriendship(DB db, int threadid, StringBuilder updateLog,StringBuilder readLog, int seqID, HashMap<String, Integer> friendshipInfo
			,HashMap<String, Integer> pendingInfo, int thinkTime,  boolean insertImage, boolean warmup)
	{		
		String actionType = "AcceptFriendship";
		int numOpsDone = 0; 
		int keyname = activateKeynameAndUpdateFreqWrite();
		if(keyname == -1) {
			return 0;
		}
		
		numOpsDone += viewProfileTransaction(db, threadid, readLog, seqID, insertImage, warmup, keyname, keyname);
		numOpsDone += viewTopKResourceTransaction(db, keyname, keyname);
		
		try {
			Thread.sleep(thinkTime);
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}
		int ret;
		
		numOpsDone += listFriendsTransaction(db, threadid, readLog, seqID, insertImage, warmup, keyname, keyname);
		
		try {
			Thread.sleep(thinkTime);
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}

		numOpsDone += viewFriendReqTransaction(db, threadid, readLog, seqID,
				insertImage, warmup, keyname);
		
		try {
			Thread.sleep(thinkTime);
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}

		if(!warmup){
			
			Vector<Integer> ids =pendingFrnds[memberIdxs.get(keyname)];
			if(ids.size() > 0){
				Random random = new Random();
				int idx = random.nextInt(ids.size());
				//int idx = random.nextInt(peResult.size());
				long startUpdatea = System.nanoTime();
				String auserid = "";
				//auserid =peResult.get(idx).get("userid").toString();
				auserid = ids.get(idx).toString();
				ret = db.acceptFriend(Integer.parseInt(auserid), keyname);
				if(ret < 0){
					System.out.println("There is an exception in acceptFriends.");
					System.exit(0);
				}
				ids.remove(idx);
				try {
					aFrnds.acquire();
					acceptedFrnds[memberIdxs.get(Integer.parseInt(auserid))].put(keyname,""); 
					acceptedFrnds[memberIdxs.get(keyname)].put(Integer.parseInt(auserid),"");
					aFrnds.release();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				int numFriendsForThisUserTillNow = 0;
				if(friendshipInfo.get(Integer.toString(keyname))!= null){
					numFriendsForThisUserTillNow = friendshipInfo.get(Integer.toString(keyname));
				}
				friendshipInfo.put(Integer.toString(keyname), (numFriendsForThisUserTillNow+1));

				int numFriendsForOtherUserTillNow = 0;
				if(friendshipInfo.get(auserid)!= null){
					numFriendsForOtherUserTillNow = friendshipInfo.get(auserid);
				}
				friendshipInfo.put(auserid, (numFriendsForOtherUserTillNow+1));
				int numPendingsForThisUserTillNow = 0;
				if(pendingInfo.get(Integer.toString(keyname))!= null){
					numPendingsForThisUserTillNow = pendingInfo.get(Integer.toString(keyname));
				}
				pendingInfo.put(Integer.toString(keyname), (numPendingsForThisUserTillNow-1));
				long endUpdatea = System.nanoTime();
				numOpsDone++;
				if(enableLogging){
					updateLog.append("UPDATE,ACCEPTFRND,"+seqID+","+threadid+",a:"+auserid+","+startUpdatea+","+endUpdatea+","+(numFriendsForOtherUserTillNow+1)+",I"+ "," + actionType +"\n");
					updateLog.append("UPDATE,ACCEPTFRND,"+seqID+","+threadid+","+keyname+","+startUpdatea+","+endUpdatea+","+(numFriendsForThisUserTillNow+1)+",I"+ "," + actionType +"\n");
					updateLog.append("UPDATE,PENDFRND,"+seqID+","+threadid+","+keyname+","+startUpdatea+","+endUpdatea+","+(numPendingsForThisUserTillNow-1)+",D"+ "," + actionType +"\n");
					updatesExist = true;
				}
				relateUsers(keyname, Integer.parseInt(auserid));
				
				try {
					Thread.sleep(thinkTime);
				} catch (InterruptedException e) {
					e.printStackTrace(System.out);
				}
				
				numOpsDone += listFriendsTransaction(db, threadid, readLog, seqID, insertImage, warmup, keyname, keyname);
				
				try {
					Thread.sleep(thinkTime);
				} catch (InterruptedException e) {
					e.printStackTrace(System.out);
				}
				
				numOpsDone += viewFriendReqTransaction(db, threadid, readLog, seqID, insertImage, warmup, keyname);
			}
		}

		deactivateUser(keyname);
		return numOpsDone;

	}

	/**
	 * @param db
	 * @param threadid
	 * @param readLog
	 * @param seqID
	 * @param insertImage
	 * @param warmup
	 * @param numOpsDone
	 * @param keyname
	 * return
	 */
	private int viewFriendReqTransaction(DB db, int threadid,
			StringBuilder readLog, int seqID, boolean insertImage,
			boolean warmup, int keyname) {
		String actionType = "ViewFriendRequests";
		int ret, numOpsDone = 0;
		Vector<HashMap<String,ByteIterator>> peResult=new Vector<HashMap<String,ByteIterator>>();
		long startReadp = System.nanoTime();
		ret = db.viewFriendReq(keyname, peResult, insertImage, false);
		if(ret < 0){
			System.out.println("There is an exception in viewFriendReq.");
			System.exit(0);
		}
		long endReadp = System.nanoTime();
		numOpsDone++;
		if(!warmup && enableLogging){
			readLog.append("READ,PENDFRND,"+seqID+","+threadid+","+keyname+","+startReadp+","+endReadp+","+peResult.size()+ "," + actionType +"\n");
			readsExist = true;
		}
		return numOpsDone;
	}

	public int doTransactionRejectFriendship(DB db, int threadid, StringBuilder updateLog, StringBuilder readLog,int seqID, HashMap<String, Integer> friendshipInfo
			,HashMap<String, Integer> pendingInfo, int thinkTime,  boolean insertImage, boolean warmup)
	{		
		String actionType = "RejectFriendship";
		int numOpsDone = 0; 
		int keyname = activateKeynameAndUpdateFreqWrite();
		if(keyname == -1) {
			return 0;
		}
		
		numOpsDone += viewProfileTransaction(db, threadid, readLog, seqID, insertImage, warmup, keyname, keyname);
		numOpsDone += viewTopKResourceTransaction(db, keyname, keyname);
		
		try {
			Thread.sleep(thinkTime);
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}
		
		numOpsDone += listFriendsTransaction(db, threadid, readLog, seqID, insertImage, warmup, keyname, keyname);
		
		try {
			Thread.sleep(thinkTime);
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}
		
		numOpsDone += viewFriendReqTransaction(db, threadid, readLog, seqID, insertImage, warmup, keyname);
		
		try {
			Thread.sleep(thinkTime);
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}
		
		if(!warmup){//TODO reject friend
			/*if(peResult.size() == 0){
				//do nothing*/
			Vector<Integer> ids =pendingFrnds[memberIdxs.get(keyname)];
			if(ids.size() > 0){
				Random random = new Random();
				int idx = random.nextInt(ids.size());
				//int idx = random.nextInt(peResult.size());
				String auserid = "";
				//auserid =peResult.get(idx).get("userid").toString();
				auserid = ids.get(idx).toString();
				long startUpdatea = System.nanoTime();
				int ret = db.rejectFriend(Integer.parseInt(auserid), keyname);
				if(ret < 0){
					System.out.println("There is an exception in rejectFriend.");
					System.exit(0);
				}
				ids.remove(idx);
				int numPendingsForThisUserTillNow = 0;
				if(pendingInfo.get(Integer.toString(keyname))!= null){
					numPendingsForThisUserTillNow = pendingInfo.get(Integer.toString(keyname));
				}
				pendingInfo.put(Integer.toString(keyname), (numPendingsForThisUserTillNow-1));
				long endUpdatea = System.nanoTime();
				numOpsDone++;
				if(enableLogging){
					updateLog.append("UPDATE,PENDFRND,"+seqID+","+threadid+","+keyname+","+startUpdatea+","+endUpdatea+","+(numPendingsForThisUserTillNow-1)+",D"+ "," + actionType +"\n");
					updatesExist = true;
				}
				deRelateUsers(keyname, Integer.parseInt(auserid) );
				
				try {
					Thread.sleep(thinkTime);
				} catch (InterruptedException e) {
					e.printStackTrace(System.out);
				}
				
				numOpsDone += listFriendsTransaction(db, threadid, readLog, seqID, insertImage, warmup, keyname, keyname);
				
				try {
					Thread.sleep(thinkTime);
				} catch (InterruptedException e) {
					e.printStackTrace(System.out);
				}

				numOpsDone += viewFriendReqTransaction(db, threadid, readLog, seqID, insertImage, warmup, keyname);
			}
		}

		
		deactivateUser(keyname);
		return numOpsDone;

	}

	public int doTransactionUnfriendFriendship(DB db, int threadid, StringBuilder updateLog,StringBuilder readLog, int seqID, HashMap<String, Integer> friendshipInfo
			,HashMap<String, Integer> pendingInfo, int thinkTime,   boolean insertImage, boolean warmup)
	{	
		int numOpsDone = 0; 
		int keyname = activateKeynameAndUpdateFreqWrite();
		if(keyname == -1) {
			return 0;
		}
		
		numOpsDone += viewProfileTransaction(db, threadid, readLog, seqID, insertImage, warmup, keyname, keyname);
		numOpsDone += viewTopKResourceTransaction(db, keyname, keyname);
		
		try {
			Thread.sleep(thinkTime);
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}
		
		numOpsDone += listFriendsTransaction(db, threadid, readLog, seqID, insertImage, warmup, keyname, keyname);
		
		if(!warmup){
			try {
				aFrnds.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace(System.out);
			}
			if(acceptedFrnds[memberIdxs.get(keyname)].size() <= 0 ){
				aFrnds.release();
			}else{
				numOpsDone += thawFriendShipTransaction(db, threadid, updateLog,
						seqID, friendshipInfo, keyname);
				try {
				Thread.sleep(thinkTime);
			} catch (InterruptedException e) {
				e.printStackTrace(System.out);
			}
		}
		
		numOpsDone += listFriendsTransaction(db, threadid, readLog, seqID, insertImage, warmup, keyname, keyname);
		}
		deactivateUser(keyname);
		return numOpsDone;
	}

	/**
	 * @param db
	 * @param threadid
	 * @param updateLog
	 * @param seqID
	 * @param friendshipInfo
	 * @param numOpsDone
	 * @param keyname
	 * return
	 */
	private int thawFriendShipTransaction(DB db, int threadid,
			StringBuilder updateLog, int seqID,
			HashMap<String, Integer> friendshipInfo, int keyname) {
		String actionType = "ThawFriendship";
		int numOpsDone = 0;
		String auserid = "";	
		Set<Integer> keys = acceptedFrnds[memberIdxs.get(keyname)].keySet();
		Iterator<Integer> it = keys.iterator();
		auserid = it.next().toString();	
		if(isActive(Integer.parseInt(auserid)) != -1){ //so two people won't delete the same friendship at the same time			
			//remove from acceptedFrnds
			acceptedFrnds[memberIdxs.get(keyname)].remove(Integer.parseInt(auserid));
			acceptedFrnds[memberIdxs.get(Integer.parseInt(auserid))].remove(keyname);
			aFrnds.release();
			long startUpdater = System.nanoTime();
			int ret = db.thawFriendship(Integer.parseInt(auserid), keyname);
			if(ret < 0){
				System.out.println("There is an exception in unFriendFriend.");
				System.exit(0);
			}
			int numFriendsForThisUserTillNow = 0;
			if(friendshipInfo.get(Integer.toString(keyname))!= null){
				numFriendsForThisUserTillNow = friendshipInfo.get(Integer.toString(keyname));
			}
			friendshipInfo.put(Integer.toString(keyname), (numFriendsForThisUserTillNow-1));

			int numFriendsForOtherUserTillNow = 0;
			if(friendshipInfo.get(auserid)!= null){
				numFriendsForOtherUserTillNow = friendshipInfo.get(auserid);
			}
			friendshipInfo.put(auserid, (numFriendsForOtherUserTillNow-1));
			long endUpdater = System.nanoTime();
			numOpsDone++;

			if(enableLogging){
				updateLog.append("UPDATE,ACCEPTFRND,"+seqID+","+threadid+",a:"+auserid+","+startUpdater+","+endUpdater+","+(numFriendsForOtherUserTillNow-1)+",D"+ "," + actionType +"\n");
				updateLog.append("UPDATE,ACCEPTFRND,"+seqID+","+threadid+","+keyname+","+startUpdater+","+endUpdater+","+(numFriendsForThisUserTillNow-1)+",D"+ "," + actionType +"\n");
				updatesExist = true;
			}
			deRelateUsers(keyname, Integer.parseInt(auserid));
			deactivateUser(Integer.parseInt(auserid));

		}
		return numOpsDone;
	}


	public int doTransactionUnfriendPendingFriendship(DB db, int threadid, StringBuilder updateLog,StringBuilder readLog, int seqID, HashMap<String, Integer> friendshipInfo
			,HashMap<String, Integer> pendingInfo, int thinkTime, String nextOp,  boolean insertImage, boolean warmup)
	{	
		String actionType = "UnfriendPendingFriendship";
		int numOpsDone = 0; 
		int keyname = activateKeynameAndUpdateFreqWrite();
		if(keyname == -1) {
			return 0;
		}
		
		numOpsDone += viewProfileTransaction(db, threadid, readLog, seqID, insertImage, warmup, keyname, keyname);
		numOpsDone += viewTopKResourceTransaction(db, keyname, keyname);
		
		try {
			Thread.sleep(thinkTime);
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}
		int ret;
		if(!warmup){
			int noRelId = -1;
			noRelId = viewNotRelatedUsers(keyname);
			if(noRelId == -1){
				//do nothing
			}else{
				if(isActive(noRelId) != -1){
					long startUpdatei = System.nanoTime();
					ret = db.inviteFriend(keyname, noRelId);	
					if(ret < 0){
						System.out.println("There is an exception in inviteFriend.");
						System.exit(0);
					}
					int numPendingsForOtherUserTillNow = 0;
					if(pendingInfo.get(Integer.toString(noRelId))!= null){
						numPendingsForOtherUserTillNow = pendingInfo.get(Integer.toString(noRelId));
					}
					pendingInfo.put(Integer.toString(noRelId), (numPendingsForOtherUserTillNow+1));
					long endUpdatei = System.nanoTime();
					numOpsDone++;
					if(enableLogging){
						updateLog.append("UPDATE,PENDFRND,"+seqID+","+threadid+","+noRelId+","+startUpdatei+","+endUpdatei+","+(numPendingsForOtherUserTillNow+1)+",I"+ "," + actionType +"\n");
						updatesExist = true;
					}
					relateUsers(keyname, noRelId);
					deactivateUser(noRelId);
					try {
						Thread.sleep(thinkTime);
					} catch (InterruptedException e) {
						e.printStackTrace(System.out);
					}
				}
			}
		}
		
		Vector<HashMap<String,ByteIterator>> fResult=new Vector<HashMap<String,ByteIterator>>();
		long startReadf = System.nanoTime();
		ret = db.listFriends(keyname, keyname, null, fResult,  insertImage, false);
		if(ret < 0){
			System.out.println("There is an exception in listFriends.");
			System.exit(0);
		}
		long endReadf = System.nanoTime();
		numOpsDone++;
		if(!warmup && enableLogging){
			readLog.append("READ,ACCEPTFRND,"+seqID+","+threadid+","+keyname+","+startReadf+","+endReadf+","+fResult.size()+ "," + actionType +"\n");
			readsExist= true;
		}
		
		if(!warmup){
			if(fResult.size() == 0){
				//do nothing
			}else{
				Random random = new Random();
				int idx = random.nextInt(fResult.size());
				long startUpdater = System.nanoTime();
				String auserid = "";
				auserid =fResult.get(idx).get("userid").toString();

				if(isActive(Integer.parseInt(auserid)) != -1){			
					ret = db.thawFriendship(Integer.parseInt(auserid), keyname);
					if(ret < 0){
						System.out.println("There is an exception in unFriendFriend.");
						System.exit(0);
					}
					int numFriendsForThisUserTillNow = 0;
					if(friendshipInfo.get(Integer.toString(keyname))!= null){
						numFriendsForThisUserTillNow = friendshipInfo.get(Integer.toString(keyname));
					}
					friendshipInfo.put(Integer.toString(keyname), (numFriendsForThisUserTillNow-1));


					int numFriendsForOtherUserTillNow = 0;
					if(friendshipInfo.get(auserid)!= null){
						numFriendsForOtherUserTillNow = friendshipInfo.get(auserid);
					}
					friendshipInfo.put(auserid, (numFriendsForOtherUserTillNow-1));
					long endUpdater = System.nanoTime();
					numOpsDone++;
					if(enableLogging){
						updateLog.append("UPDATE,ACCEPTFRND,"+seqID+","+threadid+",a:"+auserid+","+startUpdater+","+endUpdater+","+(numFriendsForOtherUserTillNow-1)+",D"+ "," + actionType +"\n");
						updateLog.append("UPDATE,ACCEPTFRND,"+seqID+","+threadid+","+keyname+","+startUpdater+","+endUpdater+","+(numFriendsForThisUserTillNow-1)+",D"+ "," + actionType +"\n");
						updatesExist = true;
					}
					deRelateUsers(keyname, Integer.parseInt(auserid));
					deactivateUser(Integer.parseInt(auserid));
					
					try {
						Thread.sleep(thinkTime);
					} catch (InterruptedException e) {
						e.printStackTrace(System.out);
					}
				}
			}
		}
		
		numOpsDone += listFriendsTransaction(db, threadid, readLog, seqID, insertImage, warmup, keyname, keyname);
		
		try {
			Thread.sleep(thinkTime);
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}

		Vector<HashMap<String,ByteIterator>> peResult=new Vector<HashMap<String,ByteIterator>>();
		long startReadp = System.nanoTime();
		ret = db.viewFriendReq(keyname, peResult, insertImage, false);
		if(ret < 0){
			System.out.println("There is an exception in viewFriendReq.");
			System.exit(0);
		}
		long endReadp = System.nanoTime();
		numOpsDone++;
		if(!warmup && enableLogging){
			readLog.append("READ,PENDFRND,"+seqID+","+threadid+","+keyname+","+startReadp+","+endReadp+","+peResult.size()+ "," + actionType +"\n");
			readsExist = true;
		}
		
		try {
			Thread.sleep(thinkTime);
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}
		if(!warmup){
			if(peResult.size() == 0){
				//do nothing
			}else{
				Random random = new Random();
				int idx = random.nextInt(peResult.size());
				long startUpdatea = System.nanoTime();
				if(nextOp.equals("ACCEPT")){
					String auserid = "";
					auserid =peResult.get(idx).get("userid").toString();
					ret = db.acceptFriend(Integer.parseInt(auserid), keyname);
					if(ret < 0){
						System.out.println("There is an exception in acceptFriends.");
						System.exit(0);
					}
					int numFriendsForThisUserTillNow = 0;
					if(friendshipInfo.get(Integer.toString(keyname))!= null){
						numFriendsForThisUserTillNow = friendshipInfo.get(Integer.toString(keyname));
					}
					friendshipInfo.put(Integer.toString(keyname), (numFriendsForThisUserTillNow+1));


					int numFriendsForOtherUserTillNow = 0;
					if(friendshipInfo.get(auserid)!= null){
						numFriendsForOtherUserTillNow = friendshipInfo.get(auserid);
					}
					friendshipInfo.put(auserid, (numFriendsForOtherUserTillNow+1));
					int numPendingsForThisUserTillNow = 0;
					if(pendingInfo.get(Integer.toString(keyname))!= null){
						numPendingsForThisUserTillNow = pendingInfo.get(Integer.toString(keyname));
					}
					pendingInfo.put(Integer.toString(keyname), (numPendingsForThisUserTillNow-1));
					long endUpdatea = System.nanoTime();
					numOpsDone++;
					if(enableLogging){
						updateLog.append("UPDATE,ACCEPTFRND,"+seqID+","+threadid+",a:"+auserid+","+startUpdatea+","+endUpdatea+","+(numFriendsForOtherUserTillNow+1)+",I"+ "," + actionType +"\n");
						updateLog.append("UPDATE,ACCEPTFRND,"+seqID+","+threadid+","+keyname+","+startUpdatea+","+endUpdatea+","+(numFriendsForThisUserTillNow+1)+",I"+ "," + actionType +"\n");
						updateLog.append("UPDATE,PENDFRND,"+seqID+","+threadid+","+keyname+","+startUpdatea+","+endUpdatea+","+(numPendingsForThisUserTillNow-1)+",D"+ "," + actionType +"\n");
						updatesExist = true;
					}
					relateUsers(keyname, Integer.parseInt(auserid));
				}else if(nextOp.equals("REJECT")){
					String auserid = "";
					auserid =peResult.get(idx).get("userid").toString();
					ret = db.rejectFriend(Integer.parseInt(auserid), keyname);
					if(ret < 0){
						System.out.println("There is an exception in rejectFriend.");
						System.exit(0);
					}
					int numPendingsForThisUserTillNow = 0;
					if(pendingInfo.get(Integer.toString(keyname))!= null){
						numPendingsForThisUserTillNow = pendingInfo.get(Integer.toString(keyname));
					}
					pendingInfo.put(Integer.toString(keyname), (numPendingsForThisUserTillNow-1));
					long endUpdatea = System.nanoTime();
					numOpsDone++;
					if(enableLogging){
						updateLog.append("UPDATE,PENDFRND,"+seqID+","+threadid+","+keyname+","+startUpdatea+","+endUpdatea+","+(numPendingsForThisUserTillNow-1)+",D"+ "," + actionType +"\n");
						updatesExist = true;
					}
					deRelateUsers(keyname, Integer.parseInt(auserid) );
				}
				
				try {
					Thread.sleep(thinkTime);
				} catch (InterruptedException e) {
					e.printStackTrace(System.out);
				}
			}
		}

		numOpsDone += listFriendsTransaction(db, threadid, readLog, seqID, insertImage, warmup, keyname, keyname);
		
		try {
			Thread.sleep(thinkTime);
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		}

		numOpsDone += viewFriendReqTransaction(db, threadid, readLog, seqID, insertImage, warmup, keyname);

		deactivateUser(keyname);
		return numOpsDone;
	}


	public int doActionGetProfile(DB db, int threadid, StringBuilder updateLog, StringBuilder readLog,int seqID, boolean insertImage,  boolean warmup)
	{		
		String actionType = "GetProfile";
		int numOpsDone = 0;
		int keyname = buildKeyName();
		keyname = activateUser(keyname);
		if(keyname == -1)
			return 0;
		incrUserRef(keyname);
		int profilekeyname = buildKeyName();
		HashMap<String,ByteIterator> pResult=new HashMap<String,ByteIterator>();
		long startReadp = System.nanoTime();
		int ret = db.viewProfile(keyname, profilekeyname, pResult, insertImage, false);
		if(ret < 0){
			System.out.println("There is an exception in getProfile.");
			System.exit(0);
		}
		long endReadp = System.nanoTime();
		numOpsDone++;
		if(!warmup && enableLogging){
			readLog.append("READ,ACCEPTFRND,"+seqID+","+threadid+","+profilekeyname+","+startReadp+","+endReadp+","+pResult.get("friendcount")+ "," + actionType +"\n");
			if(keyname == profilekeyname) {
				readLog.append("READ,PENDFRND,"+seqID+","+threadid+","+profilekeyname+","+startReadp+","+endReadp+","+pResult.get("pendingcount")+ "," + actionType +"\n");
			}
			readsExist = true;
		}
		deactivateUser(keyname);
		return numOpsDone;
	}


	public int doActionGetFriends(DB db, int threadid, StringBuilder updateLog,StringBuilder readLog, int seqID, boolean insertImage, boolean warmup)
	{
		String actionType = "GetFriends";
		int numOpsDone = 0;
		int keyname = buildKeyName();
		keyname = activateUser(keyname);
		if(keyname == -1)
			return 0;
		incrUserRef(keyname);
		int profilekeyname = buildKeyName();
		Vector<HashMap<String,ByteIterator>> fResult=new Vector<HashMap<String,ByteIterator>>();
		long startReadf = System.nanoTime();
		int ret = db.listFriends(keyname, profilekeyname, null, fResult,  insertImage, false);
		if(ret < 0){
			System.out.println("There is an exception in listFriends.");
			System.exit(0);
		}
		long endReadf = System.nanoTime();
		numOpsDone++;
		if(!warmup && enableLogging){
			readLog.append("READ,ACCEPTFRND,"+seqID+","+threadid+","+profilekeyname+","+startReadf+","+endReadf+","+fResult.size()+ "," + actionType +"\n");
			readsExist = true;
		}

		deactivateUser(keyname);
		return numOpsDone;
	}

	public int doActionGetPendings(DB db, int threadid, StringBuilder updateLog,StringBuilder readLog, int seqID, boolean insertImage, boolean warmup)
	{
		String actionType = "GetPendingFriends";
		int numOpsDone = 0;
		int keyname = buildKeyName();
		keyname = activateUser(keyname);
		if(keyname == -1)
			return 0;
		incrUserRef(keyname);
		Vector<HashMap<String,ByteIterator>> pResult=new Vector<HashMap<String,ByteIterator>>();
		long startReadf = System.nanoTime();
		int ret = db.viewFriendReq(keyname,pResult, insertImage, false);
		if(ret < 0){
			System.out.println("There is an exception in viewFriendReq.");
			System.exit(0);
		}
		long endReadf = System.nanoTime();
		numOpsDone++;
		if(!warmup && enableLogging){
			readLog.append("READ,PENDFRND,"+seqID+","+threadid+","+keyname+","+startReadf+","+endReadf+","+pResult.size()+ "," + actionType +"\n");
			readsExist = true;
		}
		deactivateUser(keyname);
		return numOpsDone;
	}

	public int doActioninviteFriend(DB db, int threadid, StringBuilder updateLog, StringBuilder readLog, int seqID, HashMap<String, Integer> friendshipInfo
			,HashMap<String, Integer> pendingInfo, boolean insertImage, boolean warmup)
	{
		String actionType = "InviteFriends";
		int numOpsDone = 0;
		int keyname = buildKeyNameWrite();
		keyname = activateUser(keyname);
		if(keyname == -1)
			return 0;
		incrUserRef(keyname);
		int noRelId = -1;
		noRelId = viewNotRelatedUsers(keyname);
		if(noRelId!= -1 && isActive(noRelId) == -1){ //not two people should invite each other at the same time
			deactivateUser(keyname);
			return numOpsDone;
		}
		if(!warmup){
			if(noRelId == -1){
				//do nothing
			}else{
				long startUpdatei = System.nanoTime();
				int ret = db.inviteFriend(keyname, noRelId);
				if(ret < 0){
					System.out.println("There is an exception in inviteFriend.");
					System.exit(0);
				}
				pendingFrnds[memberIdxs.get(noRelId)].add(keyname);
				if(enforceFriendship){
					try {
						withPend.acquire();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if(usersWithpendingFrnds.get(noRelId) == null)
						usersWithpendingFrnds.put(noRelId, 1);
					else{
						usersWithpendingFrnds.put(noRelId, usersWithpendingFrnds.get(noRelId)+1);
					}
					withPend.release();
				}
				int numPendingsForOtherUserTillNow = 0;
				if(pendingInfo.get(Integer.toString(noRelId))!= null){
					numPendingsForOtherUserTillNow = pendingInfo.get(Integer.toString(noRelId));
				}
				pendingInfo.put(Integer.toString(noRelId), (numPendingsForOtherUserTillNow+1));
				long endUpdatei = System.nanoTime();
				numOpsDone++;
				if(enableLogging){
					updateLog.append("UPDATE,PENDFRND,"+seqID+","+threadid+","+noRelId+","+startUpdatei+","+endUpdatei+","+(numPendingsForOtherUserTillNow+1)+",I"+ "," + actionType +"\n");
					updatesExist= true;
				}
				relateUsers(keyname, noRelId );
				deactivateUser(noRelId);
			}	
		}
		deactivateUser(keyname);
		return numOpsDone;
	}


	public int doActionAcceptFriends(DB db, int threadid, StringBuilder updateLog,StringBuilder readLog, int seqID, HashMap<String, Integer> friendshipInfo
			,HashMap<String, Integer> pendingInfo, int thinkTime,  boolean insertImage, boolean warmup)
	{
		String actionType = "AcceptFriend";
		int numOpsDone = 0;
		int keyname = buildKeyNameWrite();
		keyname = activateUser(keyname);
		if(keyname == -1)
			return 0;
		incrUserRef(keyname); 
		if(!warmup){
			int auserid = -1;
			Vector<Integer> ids =pendingFrnds[memberIdxs.get(keyname)];
			if(ids.size() <= 0 && enforceFriendship){
				//pick another user with pending friends that is not active
				try {
					withPend.acquire();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Set<Integer> allUsers = usersWithpendingFrnds.keySet();
				Iterator<Integer> allUsersIt = allUsers.iterator();
				if(allUsers.size() > 0){
					int newkeyname=-1;
					int explored = allUsers.size();
					while(explored >0){
						if(allUsersIt.hasNext()){
							newkeyname = allUsersIt.next();
							explored--;
						}
						if(isActive(newkeyname) != -1){
							deactivateUser(keyname);
							keyname = newkeyname;
							ids =pendingFrnds[memberIdxs.get(keyname)];
							incrUserRef(keyname);
							break;
						}
					}
				}/*else{
					db.acceptFriend(-1, -1);
					withPend.release();
					return 0;
				}*/
				withPend.release();
			}
			if(ids.size() > 0){ //should always be true unless no one has friendship
				auserid = ids.get(ids.size()-1);
				long startUpdatea = System.nanoTime();
				int ret = 0;
				ret = db.acceptFriend(auserid, keyname);
				if(ret < 0){
					System.out.println("There is an exception in acceptFriend.");
					System.exit(0);
				}
				if(enforceFriendship){
					try {
						withPend.acquire();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					int numPending = usersWithpendingFrnds.get(keyname);
					if(numPending - 1 == 0)
						usersWithpendingFrnds.remove(keyname);
					else
						usersWithpendingFrnds.put(keyname, numPending-1);
					withPend.release();
				}
				//remove from the list because it has been accepted
				ids.remove(ids.size()-1);
				try {
					aFrnds.acquire();
					acceptedFrnds[memberIdxs.get(auserid)].put(keyname,""); 
					acceptedFrnds[memberIdxs.get(keyname)].put(auserid,"");
					aFrnds.release();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	
				int numFriendsForThisUserTillNow = 0;
				if(friendshipInfo.get(Integer.toString(keyname))!= null){
					numFriendsForThisUserTillNow = friendshipInfo.get(Integer.toString(keyname));
				}
				friendshipInfo.put(Integer.toString(keyname), (numFriendsForThisUserTillNow+1));


				int numFriendsForOtherUserTillNow = 0;
				if(friendshipInfo.get(auserid)!= null){
					numFriendsForOtherUserTillNow = friendshipInfo.get(auserid);
				}
				friendshipInfo.put(Integer.toString(auserid), (numFriendsForOtherUserTillNow+1));
				
				int numPendingsForThisUserTillNow = 0;
				if(pendingInfo.get(Integer.toString(keyname))!= null){
					numPendingsForThisUserTillNow = pendingInfo.get(Integer.toString(keyname));
				}
				pendingInfo.put(Integer.toString(keyname), (numPendingsForThisUserTillNow-1));
				
				long endUpdatea = System.nanoTime();
				numOpsDone++;
				if(enableLogging){
					updateLog.append("UPDATE,ACCEPTFRND,"+seqID+","+threadid+",a:"+auserid+","+startUpdatea+","+endUpdatea+","+(numFriendsForOtherUserTillNow+1)+",I"+ "," + actionType +"\n");
					updateLog.append("UPDATE,ACCEPTFRND,"+seqID+","+threadid+","+keyname+","+startUpdatea+","+endUpdatea+","+(numFriendsForThisUserTillNow+1)+",I"+ "," + actionType +"\n");
					updateLog.append("UPDATE,PENDFRND,"+seqID+","+threadid+","+keyname+","+startUpdatea+","+endUpdatea+","+(numPendingsForThisUserTillNow-1)+",D"+ "," + actionType +"\n");
					updatesExist = true;
				}
				relateUsers(keyname,auserid);
			}
		}
		deactivateUser(keyname);
		return numOpsDone;

	}

	public int doActionRejectFriends(DB db, int threadid, StringBuilder updateLog,StringBuilder readLog, int seqID, HashMap<String, Integer> friendshipInfo
			,HashMap<String, Integer> pendingInfo, int thinkTime,  boolean insertImage, boolean warmup)
	{
		String actionType = "RejectFriend";
		int numOpsDone = 0;
		int keyname = buildKeyNameWrite();
		keyname = activateUser(keyname);
		if(keyname == -1)
			return 0;
		incrUserRef(keyname);
		if(!warmup){
			int auserid = -1;
			Vector<Integer> ids =pendingFrnds[memberIdxs.get(keyname)];
			if(ids.size() <= 0 && enforceFriendship){
				//pick another user with pending friends that is not active
				try {
					withPend.acquire();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Set<Integer> allUsers = usersWithpendingFrnds.keySet();
				if(allUsers.size() > 0){
					int explored = allUsers.size();
					Iterator<Integer> allUsersIt = allUsers.iterator();
					while(explored>0)
					{
						int newkeyname = -1;
						if(allUsersIt.hasNext()){
							newkeyname = allUsersIt.next();
							explored--;
						}
						if(isActive(newkeyname) != -1){
							deactivateUser(keyname);
							keyname = newkeyname;
							ids =pendingFrnds[memberIdxs.get(keyname)];
							incrUserRef(keyname);
							break;
						}
					}
				}
				withPend.release();
			}
			if(ids.size() > 0){ //should always be true unless no one has pending requests
				auserid = ids.get(ids.size()-1);
				int ret = 0;			
				long startUpdatea = System.nanoTime();
				ret = db.rejectFriend(auserid, keyname);
				if(ret < 0){
					System.out.println("There is an exception in rejectFriend.");
					System.exit(0);
				}
				//remove from the list coz it has been rejected
				ids.remove(ids.size()-1);
				if(enforceFriendship){
					try {
						withPend.acquire();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					int numPending = usersWithpendingFrnds.get(keyname);
					if(numPending - 1 == 0){
						usersWithpendingFrnds.remove(keyname);
					}else{
						usersWithpendingFrnds.put(keyname, numPending-1);
					}
					withPend.release();
				}
				
				int numPendingsForThisUserTillNow = 0;
				if(pendingInfo.get(Integer.toString(keyname))!= null){
					numPendingsForThisUserTillNow = pendingInfo.get(Integer.toString(keyname));
				}
				pendingInfo.put(Integer.toString(keyname), (numPendingsForThisUserTillNow-1));
				
				long endUpdatea = System.nanoTime();
				numOpsDone++;
				if(enableLogging){
					updateLog.append("UPDATE,PENDFRND,"+seqID+","+threadid+","+keyname+","+startUpdatea+","+endUpdatea+","+(numPendingsForThisUserTillNow-1)+",D"+ "," + actionType +"\n");
					updatesExist = true;
				}
				deRelateUsers(keyname, auserid );
			}
		}
		deactivateUser(keyname);
		return numOpsDone;
	}

	public int doActionUnFriendFriends(DB db, int threadid, StringBuilder updateLog,StringBuilder readLog, int seqID, HashMap<String, Integer> friendshipInfo
			,HashMap<String, Integer> pendingInfo, int thinkTime,   boolean insertImage, boolean warmup)
	{
		String actionType = "Unfriendfriend";
		int numOpsDone = 0;
		int keyname = buildKeyNameWrite();
		keyname = activateUser(keyname);
		if(keyname == -1)
			return 0;
		incrUserRef(keyname);
		if(!warmup){
			int ret = 0;
			try {
				aFrnds.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace(System.out);
			}
			if(acceptedFrnds[memberIdxs.get(keyname)].size() > 0 ){
				int auserid = -1;	
				Set<Integer> keys = acceptedFrnds[memberIdxs.get(keyname)].keySet();
				Iterator<Integer> it = keys.iterator();
				auserid = it.next();	
				if(isActive(auserid) != -1){ //two members should not delete each other at the same time
					//remove from acceptedFrnds
					acceptedFrnds[memberIdxs.get(keyname)].remove(auserid);
					acceptedFrnds[memberIdxs.get(auserid)].remove(keyname);
					aFrnds.release();
					
					long startUpdater = System.nanoTime();
					ret = db.thawFriendship(auserid, keyname);
					if(ret < 0){
						System.out.println("There is an exception in unFriendFriend.");
						System.exit(0);
					}

					int numFriendsForThisUserTillNow = 0;
					if(friendshipInfo.get(Integer.toString(keyname))!= null){
						numFriendsForThisUserTillNow = friendshipInfo.get(Integer.toString(keyname));
					}
					friendshipInfo.put(Integer.toString(keyname), (numFriendsForThisUserTillNow-1));


					int numFriendsForOtherUserTillNow = 0;
					if(friendshipInfo.get(auserid)!= null){
						numFriendsForOtherUserTillNow = friendshipInfo.get(auserid);
					}
					friendshipInfo.put(Integer.toString(auserid), (numFriendsForOtherUserTillNow-1));
					long endUpdater = System.nanoTime();
					numOpsDone++;
					if(enableLogging){
						updateLog.append("UPDATE,ACCEPTFRND,"+seqID+","+threadid+",a:"+auserid+","+startUpdater+","+endUpdater+","+(numFriendsForOtherUserTillNow-1)+",D"+ "," + actionType +"\n");
						updateLog.append("UPDATE,ACCEPTFRND,"+seqID+","+threadid+","+keyname+","+startUpdater+","+endUpdater+","+(numFriendsForThisUserTillNow-1)+",D"+ "," + actionType +"\n");
						updatesExist = true;
					}
					deRelateUsers(keyname, auserid);
					deactivateUser(auserid);
				}else 
					aFrnds.release();
			}else{
				aFrnds.release();
			}
		}
		deactivateUser(keyname);
		return numOpsDone;
	}

	public int doActionGetTopResources(DB db, int threadid, StringBuilder updateLog, StringBuilder readLog,int seqID, boolean insertImage, boolean warmup)
	{
		int numOpsDone = 0;
		int keyname = buildKeyName();
		keyname = activateUser(keyname);
		if(keyname == -1)
			return 0;
		incrUserRef(keyname);
		int profilekeyname = buildKeyName();
		Vector<HashMap<String,ByteIterator>> rResult=new Vector<HashMap<String,ByteIterator>>();		
		int ret = db.viewTopKResources(keyname, profilekeyname, 5, rResult);
		if(ret < 0){
			System.out.println("There is an exception in getTopResource.");
			System.exit(0);
		}
		numOpsDone++;
		deactivateUser(keyname);
		return numOpsDone;
	}

	public int doActionPostComments(DB db,int threadid, StringBuilder updateLog, StringBuilder readLog,int seqID, HashMap<String, Integer> resUpdateOperations, int thinkTime, boolean insertImage, boolean warmup)
	{
		//posts a comment on one of her owned resources
		String actionType = "PostComments";
		int numOpsDone = 0;
		int commentor = buildKeyNameWrite();
		commentor = activateUser(commentor);
		if(commentor == -1)
			return 0;
		incrUserRef(commentor);
		Vector<Integer> profilekeynameresources = createdResources[memberIdxs.get(commentor)];
		if(profilekeynameresources.size() > 0){
			Random random = new Random();
			int idx = random.nextInt(profilekeynameresources.size());
			//Vector<HashMap<String,ByteIterator>> cResult=new Vector<HashMap<String,ByteIterator>>();
			String resourceID = "";
			resourceID = profilekeynameresources.get(idx).toString();
			if(!warmup){
				HashMap<String,ByteIterator> commentValues = new HashMap<String, ByteIterator>(); 
				createCommentAttrs(commentValues);
				try {
					//needed for when we have a mix of actions and sessions as other members can also post comments on any resource
					sCmts.acquire();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} 
				
				int mid = maxCommentIds.get(Integer.parseInt(resourceID))+1;
				maxCommentIds.put(Integer.parseInt(resourceID), mid);
				sCmts.release();
				commentValues.put("mid", new ObjectByteIterator(Integer.toString(mid).getBytes()));
				long startUpdate = System.nanoTime();
				int ret =db.postCommentOnResource(commentor, commentor, Integer.parseInt(resourceID), commentValues); 
				if(ret < 0){
					System.out.println("There is an exception in postComment."+mid+" "+resourceID+" "+commentor+" "+postedComments.get(Integer.parseInt(resourceID)));
					System.exit(0);
				}
				long endUpdate = System.nanoTime();
				postedComments.get(Integer.parseInt(resourceID)).add(mid);
				numOpsDone++;
				int numUpdatesTillNow = 0;

				if(resUpdateOperations.get(resourceID)!= null){
					numUpdatesTillNow = resUpdateOperations.get(resourceID);
				}
				resUpdateOperations.put(resourceID, (numUpdatesTillNow+1));
				if(enableLogging){
					updateLog.append("UPDATE,POSTCOMMENT,"+seqID+","+threadid+","+resourceID+","+startUpdate+","+endUpdate+","+(numUpdatesTillNow+1)+",I"+ "," + actionType +"\n");
					updatesExist = true;
				}
			}
		}		
		deactivateUser(commentor);
		return numOpsDone;

	}	

	public int doActionDelComments(DB db,int threadid, StringBuilder updateLog, StringBuilder readLog,int seqID, HashMap<String, Integer> resUpdateOperations, int thinkTime, boolean insertImage, boolean warmup)
	{
		// a user can only delete a comment on her own resource
		String actionType = "DeleteComments";
		int numOpsDone = 0;
		int keyname = buildKeyNameWrite();
		keyname = activateUser(keyname);
		if(keyname == -1)
			return 0;
		incrUserRef(keyname);
		Vector<Integer> profilekeynameresources = createdResources[memberIdxs.get(keyname)];
		if(profilekeynameresources.size() > 0){
			Random random = new Random();
			int idx = random.nextInt(profilekeynameresources.size());
			Vector<HashMap<String,ByteIterator>> cResult=new Vector<HashMap<String,ByteIterator>>();
			String resourceID = "";
			resourceID = profilekeynameresources.get(idx).toString();
			if(!warmup){
				try {
					sCmts.acquire();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(postedComments.get(Integer.parseInt(resourceID)) != null && postedComments.get(Integer.parseInt(resourceID)).size()>0 ){
					int midx = random.nextInt(postedComments.get(Integer.parseInt(resourceID)).size());
					int mid = postedComments.get(Integer.parseInt(resourceID)).get(midx);
					postedComments.get(Integer.parseInt(resourceID)).remove(midx);
					sCmts.release();
					long startUpdate = System.nanoTime();
					int ret =db.delCommentOnResource(keyname,Integer.parseInt(resourceID), mid); 
					if(ret < 0){
						System.out.println("There is an exception in delComment.");
						System.exit(0);
					}
					
					long endUpdate = System.nanoTime();
					numOpsDone++;
					int numUpdatesTillNow = 0;

					if(resUpdateOperations.get(resourceID)!= null){
						numUpdatesTillNow = resUpdateOperations.get(resourceID);
					}
					resUpdateOperations.put(resourceID, (numUpdatesTillNow-1));
					if(enableLogging){
						updateLog.append("UPDATE,POSTCOMMENT,"+seqID+","+threadid+","+resourceID+","+startUpdate+","+endUpdate+","+(numUpdatesTillNow-1)+",D"+ "," + actionType +"\n");
						updatesExist = true;
					}
				}else
					sCmts.release();
			}
		}
		deactivateUser(keyname);
		return numOpsDone;
	}

	public int doActionviewCommentOnResource(DB db, int threadid, StringBuilder updateLog, StringBuilder readLog,int seqID, int thinkTime, boolean insertImage,  boolean warmup)
	{
		String actionType = "ViewCommentonResource";
		int numOpsDone = 0;
		int keyname = buildKeyName();
		keyname = activateUser(keyname);
		if(keyname == -1)
			return 0;
		incrUserRef(keyname);
		int profilekeyname = buildKeyName();
		//get resources for profilekeyname
		Vector<Integer> profilekeynameresources = createdResources[memberIdxs.get(profilekeyname)];
		if(profilekeynameresources.size() > 0){
			Random random = new Random();
			int idx = random.nextInt(profilekeynameresources.size());
			Vector<HashMap<String,ByteIterator>> cResult=new Vector<HashMap<String,ByteIterator>>();
			String resourceID ="";
			resourceID = profilekeynameresources.get(idx).toString();
			long startRead = System.nanoTime();
			int ret = db.viewCommentOnResource(keyname, profilekeyname, Integer.parseInt(resourceID), cResult);
			if(ret < 0){
				System.out.println("There is an exception in getResourceComment."+resourceID);
				System.exit(0);
			}
			long endRead = System.nanoTime();
			numOpsDone++;
			if(!warmup && enableLogging){
				readLog.append("READ,POSTCOMMENT,"+seqID+","+threadid+","+resourceID+","+startRead+","+endRead+","+cResult.size()+ "," + actionType +"\n");
				readsExist = true;
			}		
		}	
		deactivateUser(keyname);
		return numOpsDone;

	}


	
	@Override
	public boolean doInsert(DB db, Object threadstate) {
		return false;
	}


	public boolean isRelated(int uid1, int uid2){
		boolean related = false;
		try {
			rStat.acquire();
			if(userRelations[(memberIdxs.get(uid1))] != null){
				HashMap<Integer, String> rels = userRelations[memberIdxs.get(uid1)];
				if(rels.containsKey(uid2)){
					related= true;
				}else
					related = false;
			}else
				related = false;
			if(userRelations[memberIdxs.get(uid2)] != null){
				HashMap<Integer, String> rels = userRelations[memberIdxs.get(uid2)];
				if(rels.containsKey(uid1)){
					related = true;
				}else
					related = false;
			}else
				related = false;
			rStat.release();
		} catch (Exception e) {
			System.out.println("Error in Rels");
			e.printStackTrace(System.out);
			System.exit(-1);
		}
		return related;		
	}


	public void relateUsers(int uid1, int uid2){
		try {

			rStat.acquire();
			if(userRelations[memberIdxs.get(uid1)] != null){
				HashMap<Integer, String> rels = userRelations[memberIdxs.get(uid1)];
				if(!rels.containsKey(uid2)){
					rels.put(uid2,"");
				}
				userRelations[memberIdxs.get(uid1)]= rels;
			}else{
				HashMap<Integer, String> rels = new HashMap<Integer, String>();
				rels.put(uid2,"");
				userRelations[memberIdxs.get(uid1)]= rels;
			}
			
			if(userRelations[memberIdxs.get(uid2)] != null){
				HashMap<Integer, String> rels = userRelations[memberIdxs.get(uid2)];
				if(!rels.containsKey(uid1)){
					rels.put(uid1,"");
				}
				userRelations[memberIdxs.get(uid2)]= rels;
			}else{
				HashMap<Integer, String> rels = new HashMap<Integer, String>();
				rels.put(uid1,"");
				userRelations[memberIdxs.get(uid2)]= rels;	
			}

			rStat.release();
		} catch (Exception e) {
			System.out.println("Error in Rels");
			e.printStackTrace(System.out);
			System.exit(-1);
		}		
	}

	public void deRelateUsers(int uid1, int uid2){
		try {
			rStat.acquire();
			HashMap<Integer, String> rels = userRelations[memberIdxs.get(uid1)];
			if(rels.containsKey(uid2)){
				rels.remove(uid2);
			}
			userRelations[memberIdxs.get(uid1)] = rels;

			rels = userRelations[memberIdxs.get(uid2)];
			if(rels.containsKey(uid1)){
				rels.remove(uid1);
			}
			userRelations[memberIdxs.get(uid2)] = rels;
			rStat.release();
		} catch (Exception e) {
			System.out.println("Error in Rels");
			e.printStackTrace(System.out);
			System.exit(-1);
		}	
	}

	public int viewNotRelatedUsers(int uid){
		int key = -1;
		try{
			rStat.acquire();
			HashMap<Integer, String> rels = userRelations[memberIdxs.get(uid)];
			int id = buildKeyName() ;
			int idx = memberIdxs.get(id);
			//int idx = random.nextInt(usercount)+useroffset;
			for(int i=idx; i<idx+usercount; i++){
				if(!rels.containsKey(myMemberObjs[i%usercount].get_uid())){
					key = myMemberObjs[i%usercount].get_uid();
					break;
				}
			}
			if(key == -1)
				System.out.println("No more friends to allocate for  "+uid+" ; benchmark results invalid");
			rStat.release();	
		}catch(Exception e){
			System.out.println("Error in view not related");
			e.printStackTrace(System.out);
			System.exit(-1);
		}
		return key;
	}

	public int isActive(int uid){
		//if active return -1 
		//else return 0
		int actualIdx = memberIdxs.get(uid);
		int shardIdx = myMemberObjs[actualIdx].get_shardIdx();
		int idxInShard = myMemberObjs[actualIdx].get_idxInShard();

		try {
			uStatSemaphores[shardIdx].acquire();
			if (userStatusShards[shardIdx][idxInShard] == 'a'){
				//user is active
				uStatSemaphores[shardIdx].release();
				return -1;
			}else{
				//user is not active
				//activate it
				userStatusShards[shardIdx][idxInShard] = 'a';
				uStatSemaphores[shardIdx].release();
				return 0;
			}	
		} catch (Exception e) {
			System.out.println("Error-Cant activate any user");
			e.printStackTrace(System.out);
			System.exit(-1);
		}
		return -1;
	}

	public int activateUser(int uid)
	{
		try {
			int actualIdx = memberIdxs.get(uid);
			int shardIdx = myMemberObjs[actualIdx].get_shardIdx();
			int idxInShard = myMemberObjs[actualIdx].get_idxInShard();
			uStatSemaphores[shardIdx].acquire();
			int cnt =0; //needed for avoiding loops
			//int shardscnt = 0;
			//find a free member within this shard
			while (userStatusShards[shardIdx][idxInShard] != 'd'){
				if(cnt == userStatusShards[shardIdx].length){
					uStatSemaphores[shardIdx].release();
					/*shardscnt ++;
					if(shardscnt == numShards){ //went through all the shards once
						return -1;
					}
					shardIdx = (shardIdx+1)%numShards;
					cnt = 0;
					idxInShard = 0;
					actualIdx = numShards*idxInShard+shardIdx;
					uid = myMemberObjs[actualIdx].get_uid();
					uStatSemaphores[shardIdx].acquire();
					continue;*/
					return -1;
				}
				idxInShard = (idxInShard+1);
				idxInShard = idxInShard%userStatusShards[shardIdx].length;
				//map to actual idx
				actualIdx = numShards*idxInShard+shardIdx;
				uid = myMemberObjs[actualIdx].get_uid();
				cnt++;
			}
			userStatusShards[shardIdx][idxInShard] ='a';
			uStatSemaphores[shardIdx].release();
		} catch (Exception e) {
			System.out.println("Error-Cant activate any user");
			e.printStackTrace(System.out);
			System.exit(-1);
		}
		return uid;
	}


	public void deactivateUser(int uid)
	{
		int actualIdx = memberIdxs.get(uid);
		int shardIdx = myMemberObjs[actualIdx].get_shardIdx();
		int idxInShard = myMemberObjs[actualIdx].get_idxInShard();

		try {
			uStatSemaphores[shardIdx].acquire();
			if (userStatusShards[shardIdx][idxInShard] == 'd') {
				System.out.println("Error - The user is already deactivated");	
			}
			userStatusShards[shardIdx][idxInShard]='d'; //Mark as available
			uStatSemaphores[shardIdx].release();
		} catch (Exception e) {
			System.out.println("Error - couldnt deactivate user");
			e.printStackTrace(System.out);
			System.exit(-1);
		}

		return ;
	}

	public void incrUserRef(int uid)
	{
		int actualIdx = memberIdxs.get(uid);
		int shardIdx = myMemberObjs[actualIdx].get_shardIdx();
		int idxInShard = myMemberObjs[actualIdx].get_idxInShard();		
		try {
			uFreqSemaphores[shardIdx].acquire();
			userFreqShards[shardIdx][idxInShard]=userFreqShards[shardIdx][idxInShard]+1;  
			uFreqSemaphores[shardIdx].release();
		} catch (Exception e) {
			System.out.println("Error-Cant increament users frequency of access");
			e.printStackTrace(System.out);
			System.exit(-1);
		}
		return;
	}


	public void createCommentAttrs(HashMap<String,ByteIterator> commentValues){
		//insert random timestamp, type and content for the comment created
		String[] fieldName = {"timestamp", "type", "content"};
		for (int i = 1; i <= 3; ++i)
		{
			String fieldKey = fieldName[i-1];
			ByteIterator data;
			if(1 == i){
				Date date = new Date();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				String dateString = sdf.format(date);
				data = new ObjectByteIterator(dateString.getBytes()); // Timestamp.
			}else{
				data = new RandomByteIterator(100); // Other fields.
			}
			commentValues.put(fieldKey, data);
		}
		
	}

	@Override
	public HashMap<String, String> getDBInitialStats(DB db) {
		HashMap<String, String> stats = new HashMap<String, String>();
		stats = db.getInitialStats();
		return stats;
	}

	public static String getFrequecyStats(){
		String userFreqStats = "";
		//int sum = 0;
		for(int i=0; i<myMemberObjs.length; i++){
			userFreqStats+=myMemberObjs[i].get_uid()+" ,"+userFreqShards[myMemberObjs[i].get_shardIdx()][myMemberObjs[i].get_idxInShard()]+"\n";
			//sum += userFreqShards[myMemberObjs[i].get_shardIdx()][myMemberObjs[i].get_idxInShard()];	
		}

		return userFreqStats;
	}

}
