package edu.usc.bg;

import java.util.ArrayList;
/**
 * BG starts from this class.
 * Use to run multiple Clients at a time
 * Calls BGMainClass
 * @author vineet
 *
 */
public class Wrapper {
	public static void main(final String[] args){
		int argIndex = 1;
		while (args[argIndex].startsWith("-")) {	//Type of operation check
			if (args[argIndex].compareTo("-schema") == 0){
				schemaRun(args);
				break;
			} else if (args[argIndex].compareTo("-db") == 0) {
				argIndex++;
			}
			else if (args[argIndex].compareTo("-load") == 0){
				validation(args,"load");
				break;
			}else if (args[argIndex].equals("-P")) {
				argIndex++;
			}
			else if (args[argIndex].compareTo("-t") == 0){
				validation(args,"t");
				break;
			}else{
				Thread run = new Thread(){
					public void run(){
						BGMainClass.main(args);
					}
				};
				run.start();
				break;
			}
			argIndex++;
		}
	}
	/**
	 * Validates the command line arguments
	 * @param args arguments
	 * @param type Type of operation(load or t)
	 */
	public static void validation(final String[] args,String type){
		int argIndex = 1;
		int numClients = 0;
		while (args[argIndex].startsWith("-")) {
			if (args[argIndex].compareTo("-schema") == 0){
				argIndex++;
			} else if (args[argIndex].compareTo("-db") == 0) {
				argIndex++;
				argIndex++;
			}
			else if (args[argIndex].compareTo("-load") == 0){
				argIndex++;
			}else if (args[argIndex].equals("-P")) {
				argIndex++;
				argIndex++;
			}
			else if(args[argIndex].compareTo("-t") == 0){
				argIndex++;
			}else if(args[argIndex].compareTo("-p") == 0){
				argIndex++;
				int eq = args[argIndex].indexOf('=');
				if (eq < 0) {
					System.out.println("Invalid Input format");
					System.exit(0);
				}
				String name = args[argIndex].substring(0, eq);
				if(name.equals("machineid")){
					System.out.println("Please do not enter the machineid. Thanks!");
					System.exit(0);
				}
				if(name.equals("numclients")){
					 numClients = Integer.parseInt(args[argIndex].substring(eq + 1));
				}
				argIndex++;
				if(argIndex >= args.length-1){
					break;
				}
			}
		}
		System.out.println("Number of Clients = "+numClients);
		
			
		ArrayList<Thread> thread = new ArrayList<Thread>();  
		for(int i=0;i<numClients;i++){
			final int clientNumber = i;
			final String[] newargs = new String[args.length+2];
			for(int j=0;j<args.length;j++){
				newargs[j] = args[j];
			}
			newargs[args.length] = "-p";
			newargs[args.length+1] = "machineid="+clientNumber;
			 Thread loadThread = new Thread(){
				public void run(){					
					BGMainClass.main(newargs);
				}
			};
			thread.add(loadThread);
		}
		
		if(type.equals("load")){
			loadRun(thread);
		}else if(type.equals("t")){
			clientRun(thread);
		}
	}
	/**
	 * Creates and run the thread to load schema
	 * @param args arguments
	 */
	public static void schemaRun(final String[] args){
		Thread schema = new Thread(){
			public void run(){
				BGMainClass.main(args);
			}
		};
		schema.start();
	}
	/**
	 * Creates and run the thread to load data
	 * @param thread ArrayList holding all the threads
	 */
	public static void loadRun(ArrayList<Thread> thread){
		
		System.out.println("No of threads"+thread.size());
		
		for(Thread t:thread){
			System.out.println("thread no = "+t.getId());
			t.start();
//			try {
//				t.join();
//			
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
		}
	}
	/**
	 * Run the Client threads
	 * @param thread ArrayList holding all the threads
	 */
	public static void clientRun(ArrayList<Thread> thread){

		for(Thread t:thread){
			System.out.println("thread no = "+t.getId());
			t.start();
		}
		for(Thread t:thread){
			System.out.println("thread no = "+t.getId());
			try {
				t.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
