package ArtificialIntelligence;

import java.io.*;
import java.util.*;

public class Astar {
	
	static int size;
	static int ax,ay;
	static int bx,by;
	boolean foarray[][]=new boolean[size][size];
	boolean soarray[][]=new boolean[size][size];
	boolean obs[][]=new boolean[size][size];
	int flag=0;
	
	
	void printOutput(LinkedList<node> x) throws IOException {
		int f_cost=x.peekLast().f_cost;//change to peekLast
		LinkedList<String> output = new LinkedList<String>();
		if (flag==1) {
			//search for parent_id
			int par=x.peekLast().pid;
			output.addFirst((x.peekLast().alpha.nax+1)+" "+(x.peekLast().alpha.nay+1));
			while (x.isEmpty()==false) {
				if (x.peekLast().nid==par) {
					output.addFirst((x.peekLast().alpha.nax+1)+" "+(x.peekLast().alpha.nay+1));
					par = x.pollLast().pid;
				}
				else {
					x.pollLast();
				}
			}
		}
		else {
			output.add("-1");
		}
		printToFile(output, f_cost);
	}
	
	private void printToFile(LinkedList<String> op , int f_cost) throws IOException {
		FileWriter fr = new FileWriter("output.txt", true);
		fr.write(System.getProperty( "line.separator"));
		fr.write((f_cost) + System.getProperty( "line.separator" ));
		while(op.isEmpty()==false) {
			String s = op.poll();
			System.out.println(s);
			fr.write(s+System.getProperty( "line.separator" ));
		}
		fr.close();
	}
	
	public static void main(String[]argts) throws NumberFormatException, IOException {
		FileHandler fh = new FileHandler("input.txt");
		size=fh.size;
		ax=fh.ax-1;
		ay=fh.ay-1;
		bx=fh.bx-1;
		by=fh.by-1;
		Astar t= new Astar();
		t.setVisit();
		t.setObstacles(fh.obsx,fh.obsy);
		t.As();
	}
	
	void setVisit() {	//Initialise all avisit and bvisit to false
		for (int i=0;i<size;i++) {
			for (int j=0; j<size; j++) {
				foarray[i][j]=false;
				soarray[i][j]=false;

			}
		}
	}
	
	void setObstacles(int obsx[], int obsy[]) {	// Initialize the obstacle map
		for (int i=0;i<size;i++) {
			for (int j=0;j<size;j++) {
				obs[i][j]=false;
			}
		}
		for (int i=0;i<obsx.length;i++)	{
			obs[obsx[i]-1][obsy[i]-1]=true;
		}
	}
	
	public LinkedList sort(LinkedList stack)
	{
		ArrayList tree=new ArrayList();
		while(!stack.isEmpty())
		{
			tree.add((node)stack.poll());
			
		}
		node temp;
		ArrayList tree2=new ArrayList();
		
		for(int i=0;i<tree.size();i++)
		{
			int min=((node)tree.get(i)).f_cost;
			for(int j=i+1;j<tree.size();j++)
			{
				if(((node)tree.get(j)).f_cost<min)
				{
					min=((node)tree.get(0)).f_cost;
					temp=(node)tree.get(i);
					tree.set(i, (node)tree.get(j));
					tree.set(j, temp);
		
				}
			}
		stack.add((node)tree.get(i));	
		}

		return stack;
	}
//	int manhattan(firstobj alpha,secondobj beta)
//	{
//		int dispx= Math.abs(alpha.nax-beta.nbx);
//		int dispy=Math.abs(alpha.nay-beta.nby);
//		h_cost=dispx+dispy;
//		return h_cost;
//	}
//	
//	int chessboard(firstobj alpha, secondobj beta)
//	{
//		int dispx= Math.abs(alpha.nax-beta.nbx);
//		int dispy=Math.abs(alpha.nay-beta.nby);
//		h_cost=Math.max(dispx,dispy);
//		return h_cost;
//	}
//	
//	double euclidean(firstobj alpha,secondobj beta)
//	{
//		h_cost=(int) Math.sqrt(((alpha.nax-beta.nbx)*(alpha.nax-beta.nbx))+((alpha.nay-beta.nby)*(alpha.nay-beta.nby)));
//		return h_cost;
//	}
	
	boolean feasibleChk(firstobj alpha,secondobj beta) {
		// check if A or B are hitting a wall
		if ( alpha.nax>=size || alpha.nax<0 || alpha.nay>=size || alpha.nay<0 || beta.nbx>=size || beta.nbx<0 || beta.nby>=size || beta.nby<0) {
			return false;
		}
		// check if A or B reach an obstacle
		if (obs[alpha.nax][alpha.nay]==true || obs[beta.nbx][beta.nby]==true) {
			return false;
		}
		else {
			return true;
		}
	}
	
	void As() throws IOException 
	{
		for(int k=0;k<3;k++)
		{
		LinkedList<node> qopen = new LinkedList<node>();
		LinkedList<node> qclose = new LinkedList<node>();
		int num=0;
		
		
		int id=0;
		node n=new node(ax,ay,bx,by,k);//---------first node--------
		
		node error=new node(-1,-1,-1,-1,-1);
		
		if(n.dispx==0&&n.dispy==0)//---------if first is goal--------
		{
			qclose.add(n);
			flag=1;
			printOutput(qclose);//***********print output************
			System.exit(1);//***********exit***********
		}
		else if(obs[ax][ay]==true || obs[bx][by]==true) {	//if initial is obstacled
			qclose.add(error);
			printOutput(qclose);//printOutput(qclose);
			System.exit(1);
		}
		else
		{
			//ArrayList<node>temp1=new ArrayList();
			    qopen.add(n);
				foarray[ax][ay]=true;
				soarray[bx][by]=true;
				while(!qopen.isEmpty())//***********insert sort function************assuming it is sorted
			{
					qopen=sort(qopen);
//					System.out.println(qopen.peek().f_cost);
//					Comparator<Integer>f_cost=Collections.reverseOrder();
//					Collections.sort(qopen);
//					sort(qopen);
					if (qopen.peek().dispx==0 && qopen.peek().dispy==0) 
					{
						qclose.add(qopen.poll());
						flag=1;
						printOutput(qclose);
						System.out.println("number of nodes expanded="+"  "+num);
						break;
					}
					int parentid=qopen.peek().nid;
					for (int i=0; i<4; i++) {
						firstobj alpha=new firstobj(qopen.peek().alpha);
						secondobj beta=new secondobj(qopen.peek().beta);
						alpha.nextpos(i);
						beta.nextpos(i);
						if(feasibleChk(alpha,beta))//*********check  feasiblity***********
						{
							int add_new_node=1;
							
							//Checking if alpha and beta have been visited, i.e, node visited 
							if ( foarray[alpha.nax][alpha.nay]==true && soarray[beta.nbx][beta.nby]==true) {
								// Checking if the old node has lesser cost for open and closed
								node temp = new node(alpha, beta,qopen.peek().g_cost,/* qopen.peek().f_cost,*/k, i , id, parentid);	//Temp node for reference
								//For open
								for (int j=0;j<qopen.size();j++) {
									if(qopen.get(j).alpha.nax==alpha.nax && qopen.get(j).alpha.nay==alpha.nay && qopen.get(j).beta.nbx==beta.nbx && qopen.get(j).beta.nby==beta.nby) {
										if(qopen.get(j).f_cost>temp.f_cost) {
											qopen.remove(j);
											id+=1;
											qopen.add(new node(alpha, beta,qopen.peek().g_cost,/* qopen.peek().f_cost,*/k, i , id, parentid));
											foarray[alpha.nax][alpha.nay]=true;
											soarray[beta.nbx][beta.nby]=true;
											add_new_node=0;
											break;
										}
										add_new_node=0;
									}
								}
								//For closed
								for (int j=0;j<qclose.size();j++) {
									if(qclose.get(j).alpha.nax==alpha.nax && qclose.get(j).alpha.nay==alpha.nay && qclose.get(j).beta.nbx==beta.nbx && qclose.get(j).beta.nby==beta.nby) {
										if(qclose.get(j).f_cost>temp.f_cost) {
											qclose.remove(j);
											id+=1;
											qopen.add(new node(alpha, beta,qopen.peek().g_cost, /*qopen.peek().f_cost,*/k, i , id, parentid));
											foarray[alpha.nax][alpha.nay]=true;
											soarray[beta.nbx][beta.nby]=true;
											add_new_node=0;
											break;
										}
										add_new_node=0;
									}
								}
								
							}	//Visit Check ends
							//Check if still new node has to be added
							if (add_new_node==1) {
								id+=1;
								qopen.add(new node(alpha, beta,qopen.peek().g_cost, /*qopen.peek().f_cost,*/k, i , id, parentid));
								foarray[alpha.nax][alpha.nay]=true;
								soarray[beta.nbx][beta.nby]=true;
							}
							
						}
//						System.out.println(alpha.nax+alpha.nay+ +beta.nbx+ +beta.nby);
	//-------------------------------------------------------------------------------------------------------------------------------------------------------					
					}
					num+=1;
					qclose.add(qopen.poll());
				}
				if(qopen.isEmpty()==true)
				{
					flag=0;
					printOutput(qclose);
					break;
				}
			}
	}
}
}

//
//	private void sort(LinkedList<node> qopen) {
//		
//		
//			   }

		
	

	
							
						
						
						
						
				
				
				
				
	
