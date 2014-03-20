package ArtificialIntelligence;



	 public class firstobj {
			int oldax=0;
			int olday=0;
			int nax=0;
			int nay=0;
			
			firstobj(int x,int y)
			{
				nax=x;
				nay=y;
				oldax=x;
				olday=y;
			}
			firstobj(firstobj a)
			{
				this.nax=a.nax;
				this.nay=a.nay;
				this.oldax=a.oldax;
				this.olday=a.olday;
			}
			void nextpos(int dir) 
			{
				oldax=nax;
				olday=nay;
				if(dir==0)
				{
					nay=nay-1;
				}
				else if(dir==1)
				{
					nax=nax+1;
				}
				else if(dir==2)
				{
					nay=nay+1;
				}
				else if(dir==3)
				{
					nax=nax-1;
				}
			}

			void prevpos()
			{
				nax=oldax;
				nay=olday;
			}
	}
