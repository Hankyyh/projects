


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import oracle.sql.STRUCT;


public class populate {

    private Connection mainCon;
    Statement mainStat = null;
   
    String firebuilding_list[] = new String[10]; //Stores the entire details of each building.
    int m=0;

    public void connectDB()
    {
        try {
                // loading Oracle Driver
                System.out.print("Looking for Oracle's jdbc-odbc driver ... ");
                DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
                System.out.print("Loaded.");
            
                //url = "jdbc:oracle:thin:@localhost:1521:SWAPNIL";
                String url = "jdbc:oracle:thin:@localhost:1522:gadodia";
                String userId = "vineet89";
                String password = "Vineet_89";

                System.out.print("Connecting to DB...");
                mainCon = DriverManager.getConnection(url, userId, password);
                mainStat = mainCon.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                System.out.println("connected !!");

            } catch (Exception e) {
                    System.out.println( "Error while connecting to DB: "+ e.toString() );
                    e.printStackTrace();
                    System.exit(-1);
            }
    }
    
    
    public void populate_it(String[] args)
    {
        try{
        
        for(int i=0; i<3; i++)
        {
                if("building.xy".equals(args[i]))
                {
                    populate_building(args[i]);
                }
                else if("hydrant.xy".equals(args[i]))
                {
                    populate_firehydrant(args[i]);
                }
                else if("firebuilding.txt".equals(args[i]))
                {
                    populate_firebuilding(args[i]);
                }
                else
                {
                    System.out.println("Incorrect file name");
                    System.exit(0);
                }
        }
        }catch(Exception e)
        {
            System.out.println(e.toString());
        }
        
    }
    
    public void populate_building(String filename) throws FileNotFoundException, IOException
    {
        
        FileReader fr = new FileReader(filename);
        BufferedReader br = new BufferedReader(fr);
        
        //--------clearing table-------------------
        String sql = "DELETE FROM building";    //clearing the building table
        try{
            mainStat.executeUpdate(sql);
        }
        catch(SQLException e)
        {
            System.out.println(e.toString());
        }
        //-----------------------------------------
        
        //-------reading file------------------
        
        String temp;
        while((temp = br.readLine()) != null)
        {   
            String buildings[] = temp.split(", ",3);    //extracting the building no,name and coordinates
           
            String building_coord[] = buildings[2].split(", ");

            String coordinates;

             int i;
             coordinates = building_coord[1];
            for(i=2; i<building_coord.length; i++)
            {
                coordinates += ","+building_coord[i];
            }
                coordinates+=","+ building_coord[1]+","+building_coord[2];
            
            String mysql = "INSERT INTO building VALUES("+"'"+buildings[0]+"','"+buildings[1]+
            "',SDO_GEOMETRY(2003,NULL,NULL,SDO_ELEM_INFO_ARRAY(1,1003,1),SDO_ORDINATE_ARRAY("+
            coordinates+")))";
            
            try{
                mainStat.executeUpdate(mysql);
            }catch(SQLException e)
            {
                System.out.println("building check failed");
                System.out.println(e.toString());
            }     
        }
        fr.close();
        br.close();
        
    }
    
    
    public void populate_firebuilding(String filename) throws FileNotFoundException, IOException
    {
        FileReader fr = new FileReader(filename);
        BufferedReader br = new BufferedReader(fr);
        
        String sql = "DELETE FROM firebuilding";    //clearing the building table
        try{
            mainStat.executeUpdate(sql);
        }
        catch(SQLException e)
        {
            System.out.println(e.toString());
        }
      
        String temp;
      
        while((temp = br.readLine()) != null)   //storing building on fire names in an array
        {

              String mysql = "INSERT INTO firebuilding VALUES("+"'"+temp+"')";
               try{
                mainStat.executeUpdate(mysql);
            }catch(SQLException e)
            {
                System.out.println(e.toString());
            }   
        }
        fr.close();
        br.close();
        
       
        
        
    }
    
    
     private void populate_firehydrant(String filename) throws FileNotFoundException, IOException {
         
      FileReader fr = new FileReader(filename);
        BufferedReader br = new BufferedReader(fr);
        
        //--------clearing table-------------------
        String sql = "DELETE FROM hydrant";    //clearing the building table
        try{
            mainStat.executeUpdate(sql);
        }catch(SQLException e)
        {
            System.out.println(e.toString());
        }
        //-----------------------------------------
        
        //-------reading file------------------
        
        String temp;
        while((temp = br.readLine()) != null)
        {
            String firehydrants[] = temp.split(", ");    //extracting the building no,name and coordinates
            //String hydrant_coord[] = firehydrants[1].split(", ");
            
            String mysql = "INSERT INTO hydrant VALUES("+"'"+firehydrants[0]+"',SDO_GEOMETRY(2001,NULL,SDO_POINT_TYPE( "+
            firehydrants[1]+","+firehydrants[2]+",NULL),NULL,NULL))";
            
            try{
                mainStat.executeUpdate(mysql);
            }catch(SQLException e)
            {
                System.out.println(e.toString());
            }     
        }
        fr.close();
        br.close();
    
     }
     
     public void close()
     {
         try{
             mainStat.close();
         }
         catch(SQLException e)
         {
             System.out.println(e.toString());
         }
     }

    public static void main(String[] args) throws FileNotFoundException, IOException
    {
        populate p = new populate();
        p.connectDB();
        
        if(args.length != 3)
        {
            System.out.println("Need 3 files!");
            System.exit(0);
        }
        else
        {
            p.populate_it(args);
        }
//        p.populate_building("building.xy");
//        p.populate_firebuilding("firebuilding.txt");
//        p.populate_firehydrant("hydrant.xy");
        
        
        p.close();
    }
}
  
