package ArtificialIntelligence;

import java.io.*;
import java.util.*;
// This will be the class where all the traversal takes place
public class FileHandler {

	int size;	// Size of Matrix
	int operation_type;		// Type of Operation

	int ax,ay;	//Sets co-ordinates of a
	int bx,by;	//Sets co-ordinates of b
	int obsx[],obsy[];	//Sets co-ordinates of the obstacle

	FileHandler(String inputfile) throws NumberFormatException, IOException 	{
		FileReader fr = new FileReader(inputfile);
		BufferedReader br = new BufferedReader(fr);
		String s;
		int count = 0;
		int file_line_count=getLineCount(inputfile);

		int number_of_obstacles=(file_line_count-3);
		obsx = new int[number_of_obstacles];
		obsy = new int[number_of_obstacles];
		while ((s=br.readLine())!=null) {
			while (s.trim().length()==0) {
				s=br.readLine();
			}
			s=s.trim();
//			System.out.println(s);
			if (count==0) {
				// Get size of Game Matrix;
				size=Integer.parseInt(s);
				count++;
			}
//			else if (count==1) {
//				// Set cost for A going up
//				up=Integer.parseInt(s);
//				count++;
//			}
//			else if (count==2) {
//				// Set cost for A going right
//				right=Integer.parseInt(s);
//				count++;
//			}
//			else if (count==3) {
//				// Set cost for A going down
//				down=Integer.parseInt(s);
//				count++;
//			}
//			else if (count==4) {
//				// Set cost for A going left
//				left=Integer.parseInt(s);
//				count++;
//			}
			//Setting Locations of A and B=============================================================================
			else if (count==1)	//For A
			{
				StringBuilder holder = new StringBuilder(s.trim());
				int temp=0;
				int xorycount=0;
				while(temp<s.length()) {
					if (holder.charAt(temp)!=' ' && xorycount==0){
						ax=Integer.parseInt(holder.substring(temp, (temp+1)));
						xorycount++;
					}
					else  if(holder.charAt(temp)!=' ' && xorycount==1) {
						ay=Integer.parseInt(holder.substring(temp, (temp+1)));
						count++;
						break;
					}
					temp++;
				}
			}
			else if (count==2) //For B
			{
				StringBuilder holder = new StringBuilder(s.trim());
				int temp=0;
				int xorycount=0;
				while(temp<s.length()) {
					if (holder.charAt(temp)!=' ' && xorycount==0){
						bx=Integer.parseInt(holder.substring(temp, (temp+1)));
						xorycount++;
					}
					else  if(holder.charAt(temp)!=' ' && xorycount==1) {
						by=Integer.parseInt(holder.substring(temp, (temp+1)));
						count++;
						break;
					}
					temp++;
				}
			}
			//Setting Obstacle Locations================================================================================
			else if (count>=3) {
				if(s.trim().length()!=0) {
					StringBuilder holder = new StringBuilder(s);
					int temp=0;
					int xorycount=0;
					while(temp<s.length()) {
						if(s.charAt(temp)!=' ' && xorycount==0) {
							obsx[count-3]=Integer.parseInt(holder.substring(temp, (temp+1)));
							xorycount++;
						}
						else if(s.charAt(temp)!=' ' && xorycount==1) {
							obsy[count-3]=Integer.parseInt(holder.substring(temp, (temp+1)));
							count++;
							break;
						}
						temp++;
					}
				}
			}

			if (count==file_line_count) {
				break;
			}
		}
		
		br.close();
		fr.close();
	}

	int getLineCount(String input) throws IOException {
		int count=0;
		FileReader fr = new FileReader(input);
		BufferedReader br=new BufferedReader(fr);
		while (br.readLine()!=null) {
			//			System.out.println(s);
			count++;
		}
		fr.close();
		br.close();
		return count;
	}
}
