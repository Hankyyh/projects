package ArtificialIntelligence;


public class node {
	firstobj alpha;
	secondobj beta;
	int g_cost;
	int dispx;
	int dispy;
	int dir;
	int nid;//node_id
	int pid;//parent_node_id
	int f_cost;
	int h_cost;
	
	int manhattan(firstobj alpha,secondobj beta)
	{
		int dispx= Math.abs(alpha.nax-beta.nbx);
		int dispy=Math.abs(alpha.nay-beta.nby);
		h_cost=dispx+dispy;
		return h_cost;
	}
	
	int chessboard(firstobj alpha, secondobj beta)
	{
		int dispx= Math.abs(alpha.nax-beta.nbx);
		int dispy=Math.abs(alpha.nay-beta.nby);
		h_cost=Math.max(dispx,dispy);
		return h_cost;
	}
	
	int euclidean(firstobj alpha,secondobj beta)
	{
		h_cost=(int) Math.sqrt(((alpha.nax-beta.nbx)*(alpha.nax-beta.nbx))+((alpha.nay-beta.nby)*(alpha.nay-beta.nby)));
		return h_cost;
	}
	
	node (firstobj a,secondobj b,int g_cost,/*int f_cost,*/int k, int dir, int id, int parentid) {	// id is to track the shortest path.
		alpha = new firstobj(a);
		beta = new secondobj(b);
		this.g_cost=g_cost;
		this.dir=dir;
		newg_cost();
		calf_cost(k);
//		calcDisplacement(alpha, beta);
		nid=id;
		pid=parentid;
		int xa=a.nax;
		int ya=a.nay;
		int xb=b.nbx;
		int yb=b.nby;
		dispx = Math.abs(xa-xb);
		dispy = Math.abs(ya-yb);
	}


	//=========================initializing constructor========================
	node (int xa, int ya, int xb, int yb,int k)	{
		alpha = new firstobj(xa,ya);
		beta = new secondobj(xb,yb);
		nid=0;
		pid=-1;	// -1 means root
		g_cost=0;
		calf_cost(k);
//		calcDisplacement(alpha, beta);
		dispx = Math.abs(xa-xb);
		dispy = Math.abs(ya-yb);
	}

	//============================================================================

	void newg_cost() { // Defines the new g_cost of the given node based on directional g_costs
		if (dir==0)
		{
			g_cost+=2;
		}
		else if (dir==1)
		{
			g_cost+=2;
		}
		else if (dir==2)
		{
			g_cost+=2;
		}
		else if (dir==3)
		{
			g_cost+=2;
		}
	}
	
	void calf_cost(int k)
	{
		if(k==0)
		{
			f_cost=g_cost+euclidean(alpha,beta);
		}
		else if(k==1)
		{
			f_cost=g_cost+manhattan(alpha,beta);
		}
		else if(k==2)
		{
			f_cost=g_cost+chessboard(alpha,beta);
		}
	}

	int getf_cost()	{ // The function returns the g_cost on being called

		return f_cost;
	}

//	void calcDisplacement(A a, B b) { // Returns the Displacement between A & B in terms of |(x,y)|
//		int xa=a.nxa;
//		int ya=a.nya;
//		int xb=b.nxb;
//		int yb=b.nyb;
//		displacementx = Math.abs(xa-xb);
//		displacementy = Math.abs(ya-yb);
////		System.out.println(alpha.nxa+".."+alpha.nya+".."+beta.nxb+".."+beta.nyb+".."+displacementx+".."+displacementy);
//	}

}
