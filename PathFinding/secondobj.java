package ArtificialIntelligence;





public class secondobj {
	int oldbx=0;
	int oldby=0;
	int nbx=0;
	int nby=0;
	
	secondobj(int x,int y)
	{
		nbx=x;
		nby=y;
		oldbx=x;
		oldby=y;
	}
	secondobj(secondobj b)
	{
		this.nbx=b.nbx;
		this.nby=b.nby;
		this.oldbx=b.oldbx;
		this.oldbx=b.oldbx;
	}
	void nextpos(int dir)
	{
		oldbx=nbx;
		oldby=nby;
		if(dir==0)
		{
			nbx=nbx+1;
		}
		else if(dir==1)
		{
			nby=nby+1;
		}
		else if(dir==2)
		{
			nbx=nbx-1;
		}
		else if(dir==3)
		{
			nby=nby-1;
		}
	}

	void prevpos()
	{
		nbx=oldbx;
		nby=oldby;
	}
	

}
