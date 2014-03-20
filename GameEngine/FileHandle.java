package AI_3;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileHandle
{
	static int size;
	static  int array[]; //=new int[size*size];
	public static void main(String[] args) throws IOException{ 
		FileReader fr = new FileReader("input.txt");
		BufferedReader br = new BufferedReader(fr);
		String s;
		StringBuilder sb = new StringBuilder();
		while((s=br.readLine())!=null)
		{
			s=s.trim();
			if(s.trim().isEmpty())
			{
				assign(sb);
				int rrcount=0;//*******to count the no of r's in the config
				int ggcount=0;//******to count the no of g's in the config
				board firstBoard=new board(array,size,0,0);
				pruning first=new pruning();//******creating objc for algo
				for(int ij=0;ij<array.length;ij++)
				{
					if(array[ij]==1)
					{
						rrcount+=1;
					}
					else if(array[ij]==2)
					{
						ggcount+=1;
					}
				}
				
				if(rrcount>ggcount)
				{
					first.getbestmove(firstBoard, first.alpha, first.beta, false);
				}
				else
				{
					first.getbestmove(firstBoard, first.alpha, first.beta, true);
				}
			}
					
			int i=0;
			while(i<s.length())
			{
				sb.append(s.charAt(i));
				i++;
			}
			
		}
}
	public static void assign(StringBuilder sb)throws IOException {
	
//		size=sb.charAt(0);
		
//		sb.deleteCharAt(0);
		String s = sb.toString();
		size = Integer.parseInt(s.substring(0,1));
		array=new int [size*size];
		s=s.substring(1, s.length());
		int w;
		for(w=0;w<s.length();w++)//******1st character was the size wich is extracted so from the nxt.....
		{
			if(s.charAt(w)=='r')
			{
				array[w]=1;//********starting is 1..bt array has to start from 0..so -1...*******
			}
			if(s.charAt(w)=='g')
			{
				array[w]=2;
			}
			if(s.charAt(w)=='.')
			{
				array[w]=0;
			}
		}
		
		sb.delete(0, sb.length());
				
}
}
