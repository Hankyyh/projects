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


package edu.usc.bg.validator;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import edu.usc.bg.base.Client;

/**
 * The thread responsible for validating the read logs using the RDBMS or the interval tree created
 * @author barahman
 *
 */
public class ValidationThread extends Thread {

	private static final boolean verbose = true;
	HashMap<Integer, Integer>[] _staleSeqTracker;
	HashMap<Integer, Integer>[] _seqTracker;
	logObject[] _toProcess;
	Properties _props;
	ConcurrentHashMap<String, resourceUpdateStat> _resUpdateDetails;
	HashMap<String , Integer> _initStats = new HashMap<String, Integer>();
	Semaphore _semaphore;
	Semaphore _staleSeqSemaphore;
	Semaphore _seqSemaphore;
	TotalValidationThreadResults _finalResults;
	Set<Integer> validValues = new TreeSet<Integer>();   //will keep a track of the valid values for every read
	int _actualCount =0;
	Bucket[] _freshnessBuckets;
	int _bucketDuration=1;


	public ValidationThread(Properties props, logObject[] toBeProcessed, 
			ConcurrentHashMap<String, resourceUpdateStat> resUpdateDetails,
			HashMap<String, Integer> initStats, Semaphore semaphore,
			TotalValidationThreadResults finalResults, 
			Semaphore staleSeqSemaphore, 
			HashMap<Integer, Integer>[] staleSeqTracker, HashMap<Integer, Integer>[] seqTracker,
			Semaphore seenSeqSemaphore, int actualCount, Bucket[] freshnessBuckets, int bucketDuration) {
		_toProcess = toBeProcessed;
		_props = props;
		_resUpdateDetails = resUpdateDetails;
		_initStats = initStats;
		_semaphore = semaphore;
		_staleSeqSemaphore = staleSeqSemaphore;
		_finalResults = finalResults;
		_staleSeqTracker = staleSeqTracker;
		_seqTracker = seqTracker;
		_seqSemaphore = seenSeqSemaphore;
		_actualCount =actualCount;
		_freshnessBuckets = freshnessBuckets;
		_bucketDuration = bucketDuration;
	}

	public void run() {

		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		int numCompleted = 0; //previously completed ones


		String url = _props.getProperty(ValidationMainClass.VALIDATION_DBURL_PROPERTY,
				ValidationMainClass.VALIDATION_DBURL_PROPERTY_DEFAULT);
		String user = _props.getProperty(ValidationMainClass.VALIDATION_DBUSER_PROPERTY, ValidationMainClass.VALIDATION_DBUSER_PROPERTY_DEFAULT);
		String passwd = _props.getProperty(ValidationMainClass.VALIDATION_DBPWD_PROPERTY, ValidationMainClass.VALIDATION_DBPWD_PROPERTY_DEFAULT);
		String driver = _props.getProperty(ValidationMainClass.VALIDATION_DBDRIVER_PROPERTY,
				ValidationMainClass.VALIDATION_DBDRIVER_PROPERTY_DEFAULT);
		int tenant=0; //0 for single and 1 for multi
		int machineid =Integer.parseInt(_props.getProperty(Client.MACHINE_ID_PROPERTY, Client.MACHINE_ID_PROPERTY_DEFAULT));
		String approach = _props.getProperty(ValidationMainClass.VALIDATION_APPROACH_PROPERTY, ValidationMainClass.VALIDATION_APPROACH_PROPERTY_DEFAULT);
		if(_props.getProperty("tenant", "single").equalsIgnoreCase("single"))
			tenant = 0;
		else
			tenant = 1;
		int threadCount = Integer.parseInt(_props.getProperty(Client.THREAD_CNT_PROPERTY,Client.THREAD_CNT_PROPERTY_DEFAULT));


		if(approach.equalsIgnoreCase("RDBMS")){
			try {
				Class.forName(driver);
				conn = DriverManager.getConnection(url, user, passwd);
				stmt = conn.createStatement();
			} catch (Exception e) {
				e.printStackTrace(System.out);
			}
		}


		// get all the read operations
		try {
			//while(true){
			_semaphore.acquire();
			//process every read in the vector passed to this thread
			for(int u=0; u<_actualCount; u++) {
				logObject record = _toProcess[u];
				if(canBeFiltered(record))
						continue;
				_finalResults.incNumReadOpsProcessed();
				// get the resource id for the res
				int threadid = Integer.parseInt(record.getThreadId());
				int seqid = Integer.parseInt(record.getSeqId());
				int rid = Integer.parseInt(record.getRid());
				String start = record.getStarttime();
				String end = record.getEndtime();
				int val = Integer.parseInt(record.getValue());
				String opType = record.getMopType();
				long freshnessReadStart = Long.parseLong(start);
				long freshnessLatestUpdateEnd = 0L;
				

				if(approach.equalsIgnoreCase("RDBMS")){
					// find all the max number of updates for each thread,
					// for this rid which completed before the read started
					String query ="";
					if(tenant==0)
						query = "select * from tupdate"+machineid+"c1 where rid="+rid+" and opType='"+opType+"' and starttime<"+start+" and endtime<="+start;
					else{
						String union ="(";
						for(int i=1; i<=threadCount; i++){
							if(i!= threadCount)
								union +="select * from tupdate"+machineid+"c"+i+" UNION ALL ";
							else 
								union +="select * from tupdate"+machineid+"c"+i+") ";
						}
						query = "select * from "+union+" where rid="+rid+" and opType='"+opType+"' and starttime<"+start+" and endtime<="+start;

					}
					stmt = conn.createStatement();
					rs = stmt.executeQuery(query);
					if( _initStats.get(opType+"-"+rid) == null)
						numCompleted = 0;
					else 
						numCompleted = _initStats.get(opType+"-"+rid);
					freshnessLatestUpdateEnd = 0;
					while(rs.next()){
						if(rs.getString("updatetype").equalsIgnoreCase("I"))
							numCompleted++;
						else
							numCompleted--;
						 //needed to find the latest update completed before the read for freshness
						if(Long.parseLong(rs.getString("endtime")) > freshnessLatestUpdateEnd)
							freshnessLatestUpdateEnd = Long.parseLong(rs.getString("endtime"));
					}
					// find all those that were happening while the read was happening
					if(tenant==0)
						query = "select * from tupdate"+machineid+"c"+"1 where ((endtime<=" + end
						+ " and starttime>=" + start + ") " + "OR (starttime<"
						+ start + " and endtime>" + start + " and endtime<"
						+ end + ") " + "OR (starttime>" + start
						+ " and endtime>" + end + " and starttime<" + end
						+ ") " + "OR (starttime<" + start + " and endtime>"
						+ end + ")) and optype='" + opType + "' and rid=" + rid;

					else{
						String union ="(";
						for(int i=1; i<=threadCount; i++){
							if(i!= threadCount)
								union +="select * from tupdate"+machineid+"c"+i+" UNION ALL ";
							else 
								union +="select * from tupdate"+machineid+"c"+i+") ";
						}
						query = "select * from "+union+" where ((endtime<=" + end
								+ " and starttime>=" + start + ") " + "OR (starttime<"
								+ start + " and endtime>" + start + " and endtime<"
								+ end + ") " + "OR (starttime>" + start
								+ " and endtime>" + end + " and starttime<" + end
								+ ") " + "OR (starttime<" + start + " and endtime>"
								+ end + ")) and optype='" + opType + "' and rid=" + rid;	
					}
					if(rs != null) rs.close();	
					rs = stmt.executeQuery(query);
					List<Interval<Long>> overlapResult = new ArrayList<Interval<Long>>();
					while (rs.next()) {
						Long intervalStart = Long.parseLong(rs.getString("starttime"));
						Long intervalEnd = Long.parseLong(rs.getString("endtime"));
						Long intervalUpdateInLong = 0L;
						if(rs.getString("updatetype").equals("I"))
							intervalUpdateInLong = 1L;
						else if (rs.getString("updatetype").equals("D"))
							intervalUpdateInLong = -1L;
						overlapResult.add(new Interval<Long>(intervalStart, intervalEnd, intervalUpdateInLong));	
					}

					if (rs != null)
						rs.close();
					if (stmt != null)
						stmt.close();

					validValues.clear();
					validValues = getValidValues(start, end, numCompleted, overlapResult);
				}else{
					//query for prev completed till the start read time
					//if update end kisses read start it is considered as already completed
					List<Interval<Long>> completedResult=null;
					completedResult = _resUpdateDetails.get(opType+"-"+rid).queryIntervalTree(0, Long.parseLong(start));
					if( _initStats.get(opType+"-"+rid) == null)
						numCompleted = 0;
					else 
						numCompleted = _initStats.get(opType+"-"+rid);

					boolean hasDuplicate = false;
					freshnessLatestUpdateEnd = 0L;
					for (int j=0; j < completedResult.size(); j++){
						hasDuplicate = false;
						if(completedResult.get(j).getEnd() > freshnessLatestUpdateEnd)
							freshnessLatestUpdateEnd = completedResult.get(j).getEnd();
						//check if the completed interval has the start of the read in it
						//if it has it then it has already been counted in overlapping
						//check if the interval picked as prev completed contains the start time 
						//that means this is an interval that completes after the start and will be counted
						//in overlapping intervals
						if(completedResult.get(j).contains(Long.parseLong(start)))
							hasDuplicate = true;

						if(!hasDuplicate){
							if(completedResult.get(j).getData()== 1)
								numCompleted++;
							else
								numCompleted--;
						}
					}
					
				
					validValues.clear();
					List<Interval<Long>> overlapResult = _resUpdateDetails.get(opType+"-"+rid).queryIntervalTree(Long.parseLong(start), Long.parseLong(end));
					validValues = getValidValues(start, end, numCompleted, overlapResult);
					
				}
				// any of the overlapping ones can be either seen or not seen,
				// so the range would be as follows
				
				int freshnessBucketIdx = 0;
				boolean discard = false;
				//on updates completed before it but updates are overlapping with it
				//should be discarded
				if(freshnessLatestUpdateEnd != 0 ){
					freshnessBucketIdx = (int)((freshnessReadStart - freshnessLatestUpdateEnd)/1000000)/_bucketDuration;
					discard = true;
				}
				if(!validValues.contains(val)){  //value observed by the read is not in the computed range
					_finalResults.incNumStaleReadsreturned();
					if (verbose)
					{
						System.out.println("*Data was stale for " + opType + ": "
								+ seqid + "-" + threadid + "-" + rid
								+ ": Valid values are ("
								+ validValues
								+ "), value Read is=" + val);
					}
					try {
						_staleSeqSemaphore.acquire();
						if (_staleSeqTracker[Integer.parseInt(record.getThreadId())] == null) {
							HashMap<Integer, Integer> valLst = new HashMap<Integer, Integer>();
							valLst.put(Integer.parseInt(record.getSeqId()),-1);
							_staleSeqTracker[Integer.parseInt(record.getThreadId())]=valLst;
						} else if (_staleSeqTracker[Integer.parseInt(record.getThreadId())].get(Integer.parseInt(record.getSeqId())) == null) {
							HashMap<Integer,Integer> valLst = _staleSeqTracker[Integer.parseInt(record.getThreadId())];
							valLst.put(Integer.parseInt(record.getSeqId()),-1);
							//_staleSeqTracker[Integer.parseInt(record.getThreadId())]=valLst;
						}
						_staleSeqSemaphore.release();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					//on updates completed before it but updates are overlapping with it
					//should be discarded
					if(!discard)
					_freshnessBuckets[freshnessBucketIdx].incStaleReads();
				}else{
					//for freshness probs
					if(!discard)
					_freshnessBuckets[freshnessBucketIdx].incValidReads();
				}
			}
			_semaphore.release();
			//}

		} catch (SQLException e) {
			e.printStackTrace(System.out);
		} catch (InterruptedException e) {
			e.printStackTrace(System.out);
		} finally {
			try {
				if (rs != null) rs.close();
				if (stmt != null) 	stmt.close();
				if (conn != null)   conn.close();
			} catch (SQLException e) {
				e.printStackTrace(System.out);
			}

		}

		
	}

	private boolean canBeFiltered(logObject record) {
		
		// add sequence to sequences seen by this thread
		try {
			_seqSemaphore.acquire();
			if (_seqTracker[Integer.parseInt(record.getThreadId())] == null) {
				HashMap<Integer, Integer> valLst = new HashMap<Integer,Integer>();
				valLst.put(Integer.parseInt(record.getSeqId()),-1);
				_seqTracker[Integer.parseInt(record.getThreadId())]= valLst;
			} else if (_seqTracker[Integer.parseInt(record.getThreadId())].get(Integer.parseInt(record.getSeqId())) == null) {
				_seqTracker[Integer.parseInt(record.getThreadId())].put(Integer.parseInt(record.getSeqId()),-1);
			}
			_seqSemaphore.release();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		if(_resUpdateDetails.get(record.getMopType()+"-"+record.getRid()) == null){ //never updated
			//discard for freshness computation
			_finalResults.incPruned();
			int cmpVal =0;
			if(_initStats.get(record.getMopType()+"-"+record.getRid()) == null)
				cmpVal = 0;
			else 
				cmpVal = _initStats.get(record.getMopType()+"-"+record.getRid());
			if(Integer.parseInt(record.getValue()) != cmpVal){
				_finalResults.incNumStaleReadsreturned();
				int inVal = 0;
				if(_initStats.get(record.getMopType()+"-"+record.getRid()) == null)
					inVal = 0;
				else
					inVal = _initStats.get(record.getMopType()+"-"+record.getRid());

				if (verbose)
				{
					System.out.println("never updated case: Data was stale for " + record.getMopType() + ": "
							+ record.getSeqId() + "-" + record.getThreadId() + "-" + record.getRid()
							+ ": Range is between "
							+ inVal
							+ "-"
							+ inVal
							+ " value Read is=" + record.getValue());
					 
				}
				try {
					_staleSeqSemaphore.acquire();
					if (_staleSeqTracker[Integer.parseInt(record.getThreadId())] == null) {
						HashMap<Integer, Integer> valLst = new HashMap<Integer, Integer>();
						valLst.put(Integer.parseInt(record.getSeqId()),-1);
						_staleSeqTracker[Integer.parseInt(record.getThreadId())]=valLst;
					} else if (_staleSeqTracker[Integer.parseInt(record.getThreadId())].get(Integer.parseInt(record.getSeqId())) == null) {
						HashMap<Integer,Integer> valLst = _staleSeqTracker[Integer.parseInt(record.getThreadId())];
						valLst.put(Integer.parseInt(record.getSeqId()),-1);
						//_staleSeqTracker[Integer.parseInt(record.getThreadId())]=valLst;
					}
					_staleSeqSemaphore.release();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			return true;
		}else{
			resourceUpdateStat updateLogStat = _resUpdateDetails.get(record.getMopType()+"-"+record.getRid());
			//check if read before first update
			if(Long.parseLong(record.getEndtime()) < Long.parseLong(updateLogStat.getMinStartTime())){
				//discard for freshness calculations
				_finalResults.incPruned();
				int cmpVal = 0;
				if( _initStats.get(record.getMopType()+"-"+record.getRid()) == null  )
					cmpVal = 0;
				else
					cmpVal = _initStats.get(record.getMopType()+"-"+record.getRid());
				if(Integer.parseInt(record.getValue()) != cmpVal ){
					_finalResults.incNumStaleReadsreturned();

					if(verbose)
					{
						System.out.println("before update case: Data was stale for " + record.getMopType() + ": "
								+ record.getSeqId() + "-" + record.getThreadId() + "-" + record.getRid()
								+ ": Range is between "
								+ cmpVal
								+ "-"
								+ cmpVal
								+ " value Read is=" + record.getValue());

					}
					try {
						_staleSeqSemaphore.acquire();
						if (_staleSeqTracker[Integer.parseInt(record.getThreadId())] == null) {
							HashMap<Integer, Integer> valLst = new HashMap<Integer, Integer>();
							valLst.put(Integer.parseInt(record.getSeqId()),-1);
							_staleSeqTracker[Integer.parseInt(record.getThreadId())]=valLst;
						} else if (_staleSeqTracker[Integer.parseInt(record.getThreadId())].get(Integer.parseInt(record.getSeqId())) == null) {
							HashMap<Integer,Integer> valLst = _staleSeqTracker[Integer.parseInt(record.getThreadId())];
							valLst.put(Integer.parseInt(record.getSeqId()),-1);
							//_staleSeqTracker[Integer.parseInt(record.getThreadId())]=valLst;
						}
						_staleSeqSemaphore.release();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				return true;
			}else if(Long.parseLong(record.getStarttime()) > Long.parseLong(updateLogStat.getMaxEndTime())){ 
				//check if after last update
				long freshnessReadStart = Long.parseLong(record.getStarttime());
				long freshnessLatesUpdateEnd = Long.parseLong(updateLogStat.getMaxEndTime());
				int freshnessBucketIdx =(int) ((freshnessReadStart - freshnessLatesUpdateEnd)/1000000)/_bucketDuration; //converting to msec
				_finalResults.incPruned();
				int inVal = 0;
				if(_initStats.get(record.getMopType()+"-"+record.getRid()) == null)
					inVal = 0;
				else
					inVal =_initStats.get(record.getMopType()+"-"+record.getRid());
				if(Integer.parseInt(record.getValue()) != (Integer.parseInt(updateLogStat.getFinalVal())+inVal)){
					_finalResults.incNumStaleReadsreturned();
					if (verbose)
					{
						System.out.println("after update case: Data was stale for " + record.getMopType() + ": "
								+ record.getSeqId() + "-" + record.getThreadId() + "-" + record.getRid()
								+ ": Range is between "
								+ (Integer.parseInt(updateLogStat.getFinalVal())+inVal)
								+ "-"
								+ (Integer.parseInt(updateLogStat.getFinalVal())+inVal)
								+ " value Read is=" + record.getValue());
						 
					}
					try {
						_staleSeqSemaphore.acquire();
						if (_staleSeqTracker[Integer.parseInt(record.getThreadId())] == null) {
							HashMap<Integer, Integer> valLst = new HashMap<Integer, Integer>();
							valLst.put(Integer.parseInt(record.getSeqId()),-1);
							_staleSeqTracker[Integer.parseInt(record.getThreadId())]=valLst;
						} else if (_staleSeqTracker[Integer.parseInt(record.getThreadId())].get(Integer.parseInt(record.getSeqId())) == null) {
							HashMap<Integer,Integer> valLst = _staleSeqTracker[Integer.parseInt(record.getThreadId())];
							valLst.put(Integer.parseInt(record.getSeqId()),-1);
							//_staleSeqTracker[Integer.parseInt(record.getThreadId())]=valLst;
						}
						_staleSeqSemaphore.release();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					//add for freshness bucket
					_freshnessBuckets[freshnessBucketIdx].incStaleReads();
					
				}else{
					//add to freshness bucket
					_freshnessBuckets[freshnessBucketIdx].incValidReads();
				}
				return true;
			}		
		}
		return false;
	}

	private TreeSet<Integer> getValidValues(String start, String end,
			int numCompleted, List<Interval<Long>> overlapResult) {
		// sort the overlapping intervals based on their end time
		SortedMap<Long, Integer> endSortedIntervals = new TreeMap<Long, Integer>();
		//assuming we have one big interval that starts at time 0 and ends at "start" and has a value of numCompleted
		//so we know till "start" the value of read is definitely numCompleted
		endSortedIntervals.put(Long.parseLong(start), numCompleted);
		//scan through all the overlapping intervals and for every end timestamp find the update types completed exactly at that point
		//that is finding all the other overlapping ones that have been completed exactly at that end time for any interval
		for (Interval<Long> interval : overlapResult){
			long endTime = interval.getEnd();
			int updateType =(int)(long) interval.getData();
			if (endSortedIntervals.containsKey(endTime)){
				updateType += endSortedIntervals.get(endTime);
			}
			endSortedIntervals.put(endTime, updateType);
		}

		//now we know among the overlapping ones ending at any point of time, what the increased or decreased update value is
		//for example we know at end time x two overlapping decrements were completed and three overlapping increments
		//so the total update type completed exactly time x is +1
		TreeSet<Integer> validValues = new TreeSet<Integer>();
		//now we try to compute the overlapping updates that are completed before time x and add it 
		//to those exactly completed at x 
		//so basically for any end time we are computing all the overlapping updates that are completed 
		//till that time and then computing the range of accepted values
		//we scan this new structure to come up with all the values that are possible
		long currentEndTime = endSortedIntervals.firstKey();
		int currentValue = endSortedIntervals.get(currentEndTime); //initially its the prev completed value
		//if there are no overlaps it should see the numCompleted ones
		validValues.add(currentValue);
		long nextEndTime;
		Set<Long> endTimes = endSortedIntervals.keySet();
		Iterator<Long> itor = endTimes.iterator();
		for (itor.next(); itor.hasNext();){
			nextEndTime = itor.next();
			List<Interval<Long>> intersectedIntervals = new ArrayList<Interval<Long>>();
			//for every end time find all other overlapping ones that intersect with this one
			for (Interval<Long> interval : overlapResult){
				//get all intersected sections from currentEndTime to nextEndTime
				//the overlapping ones completed before nextEndTime
				if (interval.intersects(new Interval<String>(currentEndTime, nextEndTime, "")))
					intersectedIntervals.add(interval);
			}
			int increment = 0;
			int decrement = 0;
			//from currentEndTime to nextEndTime all updates should be done.
			for (Interval<Long> interval : intersectedIntervals) {
				if (interval.getEnd() == currentEndTime)
					continue;

				if (interval.getData()>0)
					increment++;
				else if (interval.getData()<0)
					decrement--;
			}

			for (int i = decrement; i < increment + 1; i++)
				validValues.add(currentValue + i);
			//it may only see the completed ones right before any update happens
			validValues.add(currentValue);
			currentValue += endSortedIntervals.get(nextEndTime);
			currentEndTime = nextEndTime;
		}
		return validValues;
	}
}