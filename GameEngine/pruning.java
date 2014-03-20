package AI_3;

import java.util.*;

public class pruning {/* *****ERROR CAUGHT:boardsarray getting intialized evrytym new boards r generated ******** */
	
		int alpha=-1000000;
		int beta=1000000;
		ArrayList<ArrayList<board>> AB=new ArrayList<ArrayList<board>>();
	  int getbestmove(board nBoard, int alpha,int beta, boolean IsMax)
	{
		if(board.end(nBoard)==true)//*******check if board is full*******
		{
			return board.evaluate(nBoard);//*****evaluate the value
			
		}
	
		if(IsMax)//********if player1 or Max
		{
			ArrayList<board> boardsarray= board.generateNewBoards(IsMax,nBoard);//****generate all possible new boards***
			for(board nextboard: boardsarray)//******loop thru all boards one by one
//			for(int i=0;i<boardsarray.size();i++)
			{
				int score=Math.max(alpha,getbestmove(nextboard,alpha,beta,!IsMax));//***callin min function 
				if(score>alpha)
				{
					alpha=score;
//					board bestboard=new board(nextboard);//*****to choose the best board at this point
					//?????if i save bestboard at all moves can i get the best move in the first?????
				}
				if(beta<=score)//*****to prune*******
				{
					System.out.println(nextboard.a+"  "+nextboard.b);//****printing the prune part
					break;
				}
							
			}
			boardsarray.clear();
			return alpha;
		}
		if(!IsMax)
		{
			ArrayList<board> boardsarray= board.generateNewBoards(IsMax,nBoard);//****generate all possible new boards***
			for(board nextboard: boardsarray)//******loop thru all boards one by one
			{
				beta=Math.min(alpha, getbestmove(nextboard,alpha,beta,!IsMax));//***callin max function 
				
				if(beta<=alpha)//*****to prune*******
				{
					System.out.println(nextboard.a+"  "+nextboard.b);//****printing the prune part
					break;
				}
							
			}
			boardsarray.clear();
			return beta;
		}
//		return (alpha,beta);
		return beta;
	}
//	}
	
	
	

//	boolean end(board A) {
//		
//		
//		for(int i=0;i<A.size;i++)
//		{
//			for(int j=0;j<A.size;j++)
//			{
//				if(A.Array[i][j]==0)
//					return false;
//			}
//			
//		}
//		return true;
//
//}
	
	
	
	
		
	
	

}
