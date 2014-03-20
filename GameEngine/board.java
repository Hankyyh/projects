package AI_3;
import java.util.*;

public class board {
	
	int a=0;
	int b=0;
	static int size;
	int Array[][];
	static boolean playerR[][];//*****for evaluation*****
	static boolean playerG[][];
	static int rcount;//*****contains no of connected r
	static int gcount;//*****contains no of connected g
//	static int score;
	
//	board(int size)
//	{
//		 Array=new int[size][size];
//	}
//	int Array[][]=new int[size][size];
	
	board(int[] initial,int size,int a,int b)//to initialize board****initial is array containig initial config
	{
		this.size=size;
		Array=new int[size][size];
		int k=0;
		for(int i=0;i<size;i++)
		{
			for(int j=0;j<size;j++)
			{
				Array[i][j]=initial[k];// array containing initial positions.....Board-initial board
				k++;
			}
		}
		this.a=a;
		this.b=b;
		
	}

//	board(board C)/************Y NOT WORKING********
//	{
//		for(int i=0;i<size;i++)
//		{
//			for(int j=0;j<size;j++)
//			{
//				Array[i][j]=C.Array[i][j];
////				 a=m;
////				 b=n;
//			}
//		}
//	}

	public board() {
		// TODO Auto-generated constructor stub
		Array=new int[size][size];
		
	}

	static boolean end(board A) {
		
		
		for(int i=0;i<size;i++)
		{
			for(int j=0;j<size;j++)
			{
				if(A.Array[i][j]==0)
					return false;
			}
			
		}
		return true;

}
	public static ArrayList<board> generateNewBoards(boolean IsMax,board D)//*****generating futher possible steps from previous presnt in D 
	{
//		board tempboard=new board(D);//******temp board containing previous config...now to generate possible configs
//		board tempboard=new board();
//		for(int i=0;i<size;i++)
//		{
//			for(int j=0;j<size;j++)
//			{
//				tempboard.Array[i][j]=D.Array[i][j];
//			}
//		}
		
		ArrayList<board> boardsarray=new ArrayList<board>();//*****arraylist to hold all the new boards
		
		for(int i=0;i<size;i++)
		{
			for(int j=0;j<size;j++)
			{
				if(D.Array[i][j]==0)//******reading the board to find empty space
				{
					if(IsMax)//******if max is moving next step
					{
						board tempboard=new board();
						for(int t=0;t<size;t++)
						{
							for(int z=0;z<size;z++)
							{
								tempboard.Array[t][z]=D.Array[t][z];
							}
						}
						tempboard.Array[i][j]=1;//******replacing with max step
						tempboard.a=i;//*******storing the index
						tempboard.b=j;
						boardsarray.add(tempboard);//******adding to list the new board
//						tempboard.Array[i][j]=0;//*****undo the move
//						tempboard.Array[i][j]=-1;//*****replacing it with new no..so as not to consider this position again
						
					}
					else if(!IsMax)
					{
						board tempboard=new board();
						for(int t=0;t<size;t++)
						{
							for(int z=0;z<size;z++)
							{
								tempboard.Array[t][z]=D.Array[t][z];
							}
						}
						tempboard.Array[i][j]=2;//******replacing with min step
						tempboard.a=i;//*******storing the index
						tempboard.b=j;
						boardsarray.add(tempboard);//******adding to list the new board
//						tempboard.Array[i][j]=0;
//						tempboard.Array[i][j]=-1;//*****replacing it with new no..so as not to consider this position again
						
					}
				}
			}
		}
		return boardsarray;
		
	}

	public static  int evaluate(board nBoard) //*****evaluating final value
	{
			playerR=new boolean[size][size];//****matrix for counting r values
			playerG=new boolean[size][size];//****matrix for counting g values
			int rArray[]=new int[70];//*******to keep the counts of r****
			int gArray[]=new int[70];//*****to keep the counts g*******
			int o=0;//****counter for rArray*****
			int t=0;//****counter for gArray*****
			boolean IsMax=true;//*******for passing to bfs function******true means player r i.e max player
			for(int i=0;i<playerR.length;i++)
			{
				for(int j=0;j<playerR.length;j++)
				{
					playerR[i][j]=false;
				}
			}
			for(int i=0;i<playerG.length;i++)
			{
				for(int j=0;j<playerG.length;j++)
				{
					playerG[i][j]=false;
				}
			}

			for(int i=0;i<size;i++)//******for counting
			{
				for(int j=0;j<size;j++)
				{
					if(nBoard.Array[i][j]==1)
					{
						BFS(nBoard,i,j,IsMax);
						rArray[o]=rcount;//*******inserting all counts in an array
						o++;
					}
					else if(nBoard.Array[i][j]==2)
					{
						BFS(nBoard,i,j,!IsMax);
						gArray[t]=gcount;
						t++;
					}
				}
			}
			Arrays.sort(rArray);//***sorting to gett the maz count
			Arrays.sort(gArray);
			int score=(rArray[rArray.length-1])-(gArray[gArray.length-1]);//****utility value
			return score;
		}
	static boolean Feasible(int q,int w)
	{
		if(q<0||q>=size||w<0||w>=size)
		{
			return false;
		}
		else {return true;}
	}

	private static void BFS(board nBoard, int i, int j,boolean s) 
	{
		if(s)
		{
			if(playerR[i][j]==true)
				return;
		}
		if(!s)
		{
			if(playerG[i][j]==true)
				return;
		}
		
		LinkedList<node> open=new LinkedList<node>();//*******for storing 
		open.add(new node(i,j));
		rcount=0;
		gcount=0;
		if(s)
		{
			open.add(new node(i,j));
			playerR[i][j]=true;
			rcount+=1;//********no of r connected*******
			while(!open.isEmpty())
			{
				int k=open.peekFirst().i;//******retrieving the value of i from 1st node*****
				int p=open.peekFirst().j;//******retrieving the value of j from 1st node*****
				for(int pos=0;pos<4;pos++)//****for direction*****
				{
					if(pos==0)
					{
						k=k-1;//**********************moving up*******
						if(Feasible(k,p))
						{
							if(nBoard.Array[k][p]==1)//********checking with board for r*****
							{
							playerR[k][p]=true;
							rcount+=1;
							open.add(new node(k,p));
							}
						}
						
					}
					if(pos==1)
					{
						k=k+1;//**********************moving down*******
						if(Feasible(k,p))
						{
							if(nBoard.Array[k][p]==1)//********checking with board for r*****
							{
							playerR[k][p]=true;
							rcount+=1;
							open.add(new node(k,p));
							}
						}
					}
					if(pos==2)
					{
						p=p+1;//**********************moving right*******
						if(Feasible(k,p))
						{	
							if(nBoard.Array[k][p]==1)//********checking with board for r*****
							{
							playerR[k][p]=true;
							rcount+=1;
							open.add(new node(k,p));
							}
						}
					}
					if(pos==3)
					{
						p=p-1;//**********************moving left*******
						if(Feasible(k,p))
						{
							if(nBoard.Array[k][p]==1)//********checking with board for r*****
							{
							playerR[k][p]=true;
							rcount+=1;
							open.add(new node(k,p));
							}
						}
					}
				}
				open.pollFirst();
			}
		}
		else if(!s)
		{
			open.add(new node(i,j));
			playerR[i][j]=true;
			 gcount+=1;
			while(!open.isEmpty())
			{
				int k=open.peekFirst().i;//******retrieving the value of i from 1st node*****
				int p=open.peekFirst().j;//******retrieving the value of j from 1st node*****
				for(int pos=0;pos<4;pos++)//****for direction*****
				{
					if(pos==0)
					{
						k=k-1;//**********************moving up*******
						if(Feasible(k,p))
						{
							if(nBoard.Array[k][p]==1)//********checking with board for g*****
							{
							playerR[k][p]=true;
							gcount+=1;
							open.add(new node(k,p));
							}
						}
					}
					if(pos==1)
					{
						k=k+1;//**********************moving down*******
						if(Feasible(k,p))
						{
							if(nBoard.Array[k][p]==1)//********checking with board for g*****
							{
							playerR[k][p]=true;
							gcount+=1;
							open.add(new node(k,p));
							}
						}
					}
					if(pos==2)
					{
						p=p+1;//**********************moving right*******
						if(Feasible(k,p))
						{
							if(nBoard.Array[k][p]==1)//********checking with board for g*****
							{
							playerR[k][p]=true;
							gcount+=1;
							open.add(new node(k,p));
							}
						}
					}
					if(pos==3)
					{
						p=p-1;//**********************moving left*******
						if(Feasible(k,p))
						{
							if(nBoard.Array[k][p]==1)//********checking with board for g*****
							{
							playerR[k][p]=true;
							gcount+=1;
							open.add(new node(k,p));
							}
						}
					}
				}
				open.pollFirst();
			}
		
		}
		
		
	
			
			
	}	
}
