/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import oracle.sdoapi.OraSpatialManager;

import oracle.sdoapi.adapter.GeometryAdapter;
import oracle.sdoapi.adapter.GeometryInputTypeNotSupportedException;
import oracle.sdoapi.geom.CoordPoint;
import oracle.sdoapi.geom.Geometry;
import oracle.sdoapi.geom.InvalidGeometryException;
import oracle.sdoapi.geom.LineString;
import oracle.sql.STRUCT;

/**
 *
 * @author Vineet
 */
public class hw2 extends javax.swing.JFrame {

    Connection mainCon = null;
    Statement mainStat = null;
    ResultSet mainResultset = null;
    ResultSetMetaData meta = null;
    
    GeometryAdapter geoAdapter;
  
    
    boolean wholeRegion = false;
    boolean rangeQuery = false;
    boolean flags= false;
    boolean mouseClicked;    //to keep track if mouse is clicked
    Point points[] = new Point[1000];
    int npoints;
    boolean selectregion;   //if polygon needs to be drawn
    boolean selectpoint ;    //if point needs to be selected
    boolean done;
    int selection;  //which radio button is checked
    String query;
    String poly_ordinates;  //global string holding the points query
    //CoordPoint nxy[][];
    String ordinates = "";
    int queryNo=1;
    boolean checkifRange = false;
    String query1;
    
     //ResultSetMetaData meta = null;
     STRUCT shape;
     Geometry geo;
     GeometryAdapter geomAdap;
     oracle.sdoapi.geom.Polygon polygon = null;
    
    
    public hw2() {
        connectDB();
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mbuttonGroup1 = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jCheckBox1 = new javax.swing.JCheckBox();
        jCheckBox2 = new javax.swing.JCheckBox();
        mRadioButton1 = new javax.swing.JRadioButton();
        mRadioButton2 = new javax.swing.JRadioButton();
        mRadioButton3 = new javax.swing.JRadioButton();
        mRadioButton4 = new javax.swing.JRadioButton();
        jButton1 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        mTextArea1 = new javax.swing.JTextArea();
        jLabel5 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jCheckBox3 = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Vineet Gadodia 3075-3595-01");
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 57, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(763, 586, 57, -1));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("map.jpg"))); // NOI18N
        jLabel1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                imageMouseClicked(evt);
            }
        });
        jLabel1.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                mousePosition(evt);
            }
        });
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        jCheckBox1.setText("Buildings");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });
        getContentPane().add(jCheckBox1, new org.netbeans.lib.awtextra.AbsoluteConstraints(880, 50, -1, -1));

        jCheckBox2.setText("Buildings on Fire");
        jCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox2ActionPerformed(evt);
            }
        });
        getContentPane().add(jCheckBox2, new org.netbeans.lib.awtextra.AbsoluteConstraints(880, 80, -1, -1));

        mbuttonGroup1.add(mRadioButton1);
        mRadioButton1.setMnemonic('a');
        mRadioButton1.setSelected(true);
        mRadioButton1.setText("Whole Region");
        mRadioButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mRadioButton1ActionPerformed(evt);
            }
        });
        getContentPane().add(mRadioButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(890, 234, -1, -1));

        mbuttonGroup1.add(mRadioButton2);
        mRadioButton2.setMnemonic('b');
        mRadioButton2.setText("Range Query");
        mRadioButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mRadioButton2ActionPerformed(evt);
            }
        });
        getContentPane().add(mRadioButton2, new org.netbeans.lib.awtextra.AbsoluteConstraints(890, 257, -1, -1));

        mbuttonGroup1.add(mRadioButton3);
        mRadioButton3.setMnemonic('c');
        mRadioButton3.setText("Find Neighbour Building");
        mRadioButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mRadioButton3ActionPerformed(evt);
            }
        });
        getContentPane().add(mRadioButton3, new org.netbeans.lib.awtextra.AbsoluteConstraints(890, 280, -1, -1));

        mbuttonGroup1.add(mRadioButton4);
        mRadioButton4.setMnemonic('d');
        mRadioButton4.setText("Find Closets Fire Hydrants");
        mRadioButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mRadioButton4ActionPerformed(evt);
            }
        });
        getContentPane().add(mRadioButton4, new org.netbeans.lib.awtextra.AbsoluteConstraints(890, 303, -1, -1));

        jButton1.setText("Submit Query");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        getContentPane().add(jButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(850, 480, 203, -1));

        jLabel2.setText("Active Feature Type");
        getContentPane().add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(877, 20, 146, -1));

        jLabel3.setText("Query");
        getContentPane().add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(890, 202, 137, -1));

        jLabel4.setText("X");
        getContentPane().add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(194, 600, -1, -1));

        mTextArea1.setColumns(30);
        mTextArea1.setLineWrap(true);
        mTextArea1.setRows(100);
        mTextArea1.setTabSize(10);
        jScrollPane1.setViewportView(mTextArea1);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 632, 900, 70));

        jLabel5.setText("Y");
        getContentPane().add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(234, 600, -1, -1));

        jLabel7.setText("Current mouse location:");
        getContentPane().add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 600, 146, -1));

        jCheckBox3.setText("Hydrants");
        jCheckBox3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox3ActionPerformed(evt);
            }
        });
        getContentPane().add(jCheckBox3, new org.netbeans.lib.awtextra.AbsoluteConstraints(880, 110, -1, -1));

        pack();
    }// </editor-fold>//GEN-END:initComponents


    private void mousePosition(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mousePosition
        
        jLabel4.setText("("+evt.getX()+",");
        jLabel5.setText(evt.getY()+")");
        
    }//GEN-LAST:event_mousePosition

    private void imageMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_imageMouseClicked
      Graphics2D graphics = (Graphics2D)jLabel1.getGraphics();
        if(mouseClicked)
        {
            if(evt.getButton() == MouseEvent.BUTTON1 && selectregion && !done)
            {
                points[npoints++] = evt.getPoint();
                
            }
            else if ((evt.getButton() == MouseEvent.BUTTON3 || evt.getButton() == MouseEvent.BUTTON2) && selectregion && !done) {
                    done = true;
                    points[npoints++] = points[0];
            }
            else if (evt.getButton() == MouseEvent.BUTTON1 && done) {
                    resetall();
                    done = false;
                    npoints = 0;
                    points[npoints++] = evt.getPoint();
            }
            else if (evt.getButton()==MouseEvent.BUTTON1 && selectpoint) {

                    points[npoints++] = evt.getPoint();
                    
                     graphics.setColor(Color.RED);
              
                      graphics.fillRect((int) points[npoints-1].x, (int) points[npoints-1].y,2 , 2);
              
            }
           if(checkifRange == true)
           {
            graphics.setColor(Color.red);
                for (int i = 0; i < npoints - 1; i++) {
                    graphics.drawLine(points[i].x, points[i].y, points[i + 1].x, points[i + 1].y);
                }
           }
        }
        
        
    }//GEN-LAST:event_imageMouseClicked

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        Graphics2D graphics = (Graphics2D)jLabel1.getGraphics();  
        if(checkifRange == true)
        {
        graphics.setColor(Color.red);
        for (int i = 0; i < npoints - 1; i++) {
            graphics.drawLine(points[i].x, points[i].y, points[i + 1].x, points[i + 1].y);
        }
        }
        
      
        selection = mbuttonGroup1.getSelection().getMnemonic();
  
        if(selection == 65)
        { 
                
            if(jCheckBox1.isSelected())
            {
                    
                   getwholebuilding();
                   retrieve_query(queryNo++); 
                   
            }
            
            if(jCheckBox2.isSelected())
            {
                   
                    getfirebuilding();
                    retrieve_query(queryNo++); 
                
            }
           
            if(jCheckBox3.isSelected())
            {
                getfirehydrantwhole();
                retrieve_query(queryNo++); 
            }
           
             setQuery();
                       
        }
        if(selection == 66)
        {

                
            if(poly_ordinate())
            {
            if(jCheckBox1.isSelected())
            {
                
                getbuildingRange(poly_ordinates);
                retrieve_query(queryNo++); 

            }
            if(jCheckBox2.isSelected())
            {
                getbuildingOnFireRange(poly_ordinates);
                retrieve_query(queryNo++); 

            }
            if(jCheckBox3.isSelected())
            {
                getFireHydrants(poly_ordinates);
                retrieve_query(queryNo++); 
               
            }
            setQuery();

            }

        }
        
        if(selection == 67)
        {

            getquery3();
            retrieve_query(queryNo++);
            setQuery();

        }
        if(selection == 68)
        {
            
               
                getquery4();
                selectpoint = false;
                flags = false;
                
              

        }   
    }//GEN-LAST:event_jButton1ActionPerformed

    private void mRadioButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mRadioButton1ActionPerformed
        // TODO add your handling code here:
        resetall();
        checkifRange = false;
    }//GEN-LAST:event_mRadioButton1ActionPerformed

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        // TODO add your handling code here:
        jLabel1.repaint();
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void jCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox2ActionPerformed
        // TODO add your handling code here:
       jLabel1.repaint();
       
    }//GEN-LAST:event_jCheckBox2ActionPerformed

    private void jCheckBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox3ActionPerformed
        // TODO add your handling code here:
            jLabel1.repaint();
    }//GEN-LAST:event_jCheckBox3ActionPerformed

    private void mRadioButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mRadioButton2ActionPerformed
        // TODO add your handling code here:
        resetall();
        mouseClicked = true;
        selectregion = true;
        checkifRange = true;
    }//GEN-LAST:event_mRadioButton2ActionPerformed

    private void mRadioButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mRadioButton3ActionPerformed
        // TODO add your handling code here:
        resetall();
        checkifRange = false;
        
    }//GEN-LAST:event_mRadioButton3ActionPerformed

    private void mRadioButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mRadioButton4ActionPerformed

        if(flags == false)
        {
            resetall();
            
        }
        flags = true;
//        getfirebuilding();
        selectpoint = true;
        mouseClicked = true;
        checkifRange = false;
    }//GEN-LAST:event_mRadioButton4ActionPerformed

    public boolean poly_ordinate()
    {
        ordinates ="";
        if(npoints > 1)
        {
            for(int i = 0; i < npoints - 1; i++)
            {
                ordinates += points[i].x + "," + points[i].y + ", ";
            }
            ordinates += points[0].x + "," + points[0].y + ")";
        
        poly_ordinates = "SDO_GEOMETRY("+ "2003,"
                    + "NULL,NULL,"
                    + "SDO_ELEM_INFO_ARRAY(1,1003,1),SDO_ORDINATE_ARRAY("
                    +ordinates+")"; 

        return true;
        }
        else{
            return false;
        }
    }
       
      private void getwholebuilding()
    {
        
        {
        CoordPoint xy[][];
        Graphics2D g = (Graphics2D)jLabel1.getGraphics();
        try {
            
          query = "SELECT * FROM building";
          mainResultset =mainStat.executeQuery(query);
          
          meta = mainResultset.getMetaData();

          geomAdap = OraSpatialManager.getGeometryAdapter("SDO", "9", STRUCT.class, null, null, mainCon);

              
                mainResultset.last();
              xy= new CoordPoint[mainResultset.getRow()][];
              mainResultset.beforeFirst();
              
              while(mainResultset.next()) 
              {
                    shape= (STRUCT) mainResultset.getObject("buildingShape");
                try {
                    geo =geomAdap.importGeometry(shape);
                } catch (InvalidGeometryException ex) {
                    Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
                } catch (GeometryInputTypeNotSupportedException ex) {
                    Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
                }
                    if (geo instanceof oracle.sdoapi.geom.Polygon)
                    {
                        oracle.sdoapi.geom.Polygon polygon = (oracle.sdoapi.geom.Polygon) geo;

                        xy[mainResultset.getRow() - 1] = polygon.getRingAt(0).getPointArray();
                    }
                }

              g.setColor(Color.YELLOW);
                int j;
                for (int i = 0; i < xy.length; i++) {
                    for (j = 0; j < xy[i].length - 1; j++) {
                        g.drawLine((int) xy[i][j].getX(), (int) xy[i][j].getY(), (int) xy[i][j + 1].getX(), (int) xy[i][j + 1].getY());
                    }
                } 

        } catch (SQLException ex) {
            Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
        }
        }
        
              
    }
      
      public void getfirehydrantwhole()
      {
          Graphics2D g = (Graphics2D)jLabel1.getGraphics();
          double xy[][];
          
          try {
            query = "SELECT * from hydrant";
            mainResultset = mainStat.executeQuery(query);
            meta = mainResultset.getMetaData();
            
            geomAdap = OraSpatialManager.getGeometryAdapter("SDO", "9", STRUCT.class, null, null, mainCon);
            mainResultset.last();
            
                xy = new double[mainResultset.getRow()][2];
              mainResultset.beforeFirst();
              
              while (mainResultset.next()) 
              {
                  shape =(STRUCT) mainResultset.getObject("hydrantLoc");
                  geo =geomAdap.importGeometry(shape);
                   if(geo instanceof oracle.sdoapi.geom.Point)
                   {
                        oracle.sdoapi.geom.Point point = (oracle.sdoapi.geom.Point) geo;
                        xy[mainResultset.getRow() - 1][0] = point.getX();
                        xy[mainResultset.getRow() - 1][1] = point.getY();
                      
                    }
              }
               g.setColor(Color.GREEN);
               for (int i = 0; i < xy.length; i++) 
               {
                      g.fillRect((int) xy[i][0] - 5, (int) xy[i][1] - 5, 15, 15);
               }
            
            
        } catch (SQLException ex) {
            Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidGeometryException ex) {
            Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GeometryInputTypeNotSupportedException ex) {
            Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
        }
          
      }
     public void getbuildingRange(String point_query)
         {
                  
            CoordPoint[][] xy;
            Graphics2D g = (Graphics2D)jLabel1.getGraphics();
       try {
           query = "SELECT b.buildingShape from building b"+" "+
                   "WHERE sdo_geom.relate(b.buildingShape,'determine',"
                   +point_query+",0.005)"
                   +"IN ('OVERLAPBDYINTERSECT', 'INSIDE', 'COVEREDBY', 'EQUAL')";
    
           mainResultset = mainStat.executeQuery(query);
      
             meta = mainResultset.getMetaData();
            geomAdap = OraSpatialManager.getGeometryAdapter("SDO", "9", STRUCT.class, null, null, mainCon);
            mainResultset.last();
            
            xy = new CoordPoint[mainResultset.getRow()][];
            mainResultset.beforeFirst();
            
            while(mainResultset.next()) {
            shape= (STRUCT)mainResultset.getObject("buildingShape");
            geo = geomAdap.importGeometry(shape);
            if (geo instanceof oracle.sdoapi.geom.Polygon)
            {
                 oracle.sdoapi.geom.Polygon polygon = (oracle.sdoapi.geom.Polygon) geo;
                 xy[mainResultset.getRow() - 1] = polygon.getRingAt(0).getPointArray();
            }
            }
            
            g.setColor(Color.YELLOW);
                int j;
                for (int i = 0; i < xy.length; i++) {
                    for (j = 0; j < xy[i].length - 1; j++) {
                        g.drawLine((int) xy[i][j].getX(), (int) xy[i][j].getY(), (int) xy[i][j + 1].getX(), (int) xy[i][j + 1].getY());
                    }
                } 
           
                      
             
       } catch (SQLException ex) {
           Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
       } catch (InvalidGeometryException ex) {
           Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
       } catch (GeometryInputTypeNotSupportedException ex) {
           Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
       }
//       try {
//          // mainResultset.close();
//       } catch (SQLException ex) {
//           Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
//       }
//        return xy;
           
            
     }
       
     public void getfirebuilding()
     {
         CoordPoint xy[][];
        Graphics2D g = (Graphics2D)jLabel1.getGraphics();
        try {
            
          query = "SELECT b.buildingShape FROM building b, firebuilding f WHERE "
                  +"f.buildingName = b.buildingName";
          mainResultset =mainStat.executeQuery(query);
          
          meta = mainResultset.getMetaData();

          geomAdap = OraSpatialManager.getGeometryAdapter("SDO", "9", STRUCT.class, null, null, mainCon);

              
                mainResultset.last();
              xy= new CoordPoint[mainResultset.getRow()][];
              mainResultset.beforeFirst();
              
              while(mainResultset.next()) 
              {
                    shape= (STRUCT) mainResultset.getObject("buildingShape");
                try {
                    geo =geomAdap.importGeometry(shape);
                } catch (InvalidGeometryException ex) {
                    Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
                } catch (GeometryInputTypeNotSupportedException ex) {
                    Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
                }
                    if (geo instanceof oracle.sdoapi.geom.Polygon)
                    {
                        oracle.sdoapi.geom.Polygon polygon = (oracle.sdoapi.geom.Polygon) geo;

                        xy[mainResultset.getRow() - 1] = polygon.getRingAt(0).getPointArray();
                    }
                }
              g.setColor(Color.RED);
              g.setStroke(new BasicStroke(3));
                int j;
                for (int i = 0; i < xy.length; i++) {
                    for (j = 0; j < xy[i].length - 1; j++) {
                        g.drawLine((int) xy[i][j].getX(), (int) xy[i][j].getY(), (int) xy[i][j + 1].getX(), (int) xy[i][j + 1].getY());
                    }
                } 
              
        } catch (SQLException ex) {
            Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
        }
              
     }
         
     
     public void getbuildingOnFireRange(String point_query)
     {
          CoordPoint[][] xy;
            Graphics2D g = (Graphics2D)jLabel1.getGraphics();
       try {
           query = "SELECT b.buildingShape from building b, firebuilding f"+" "+
                   "WHERE f.buildingName = b.buildingName AND sdo_geom.relate(b.buildingShape,'determine',"
                   +point_query+",0.005)"
                   +"IN ('OVERLAPBDYINTERSECT', 'INSIDE', 'COVEREDBY', 'EQUAL')";
        
           mainResultset = mainStat.executeQuery(query);
           
           
             meta = mainResultset.getMetaData();
            geomAdap = OraSpatialManager.getGeometryAdapter("SDO", "9", STRUCT.class, null, null, mainCon);
            mainResultset.last();
            
            xy = new CoordPoint[mainResultset.getRow()][];
            mainResultset.beforeFirst();
            
            while(mainResultset.next()) {
            shape= (STRUCT)mainResultset.getObject("buildingShape");
            geo = geomAdap.importGeometry(shape);
            if (geo instanceof oracle.sdoapi.geom.Polygon)
            {
                 oracle.sdoapi.geom.Polygon polygon = (oracle.sdoapi.geom.Polygon) geo;
                 xy[mainResultset.getRow() - 1] = polygon.getRingAt(0).getPointArray();
            }
            }
            
            g.setColor(Color.RED);
            g.setStroke(new BasicStroke(3));
                int j;
                for (int i = 0; i < xy.length; i++) {
                    for (j = 0; j < xy[i].length - 1; j++) {
                        g.drawLine((int) xy[i][j].getX(), (int) xy[i][j].getY(), (int) xy[i][j + 1].getX(), (int) xy[i][j + 1].getY());
                    }
                } 
           
                      
             
       } catch (SQLException ex) {
           Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
       } catch (InvalidGeometryException ex) {
           Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
       } catch (GeometryInputTypeNotSupportedException ex) {
           Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
       }
     }
            
            public void getFireHydrants(String point_query)
            {
               Graphics2D g = (Graphics2D)jLabel1.getGraphics();
               double xy[][];
        try {
            query = "SELECT p.hydrantLoc FROM hydrant p "
                    + "WHERE sdo_geom.relate(p.hydrantLoc,'determine', "
                    +point_query+",0.005) IN ('INSIDE')";
           
            mainResultset = mainStat.executeQuery(query);
            meta = mainResultset.getMetaData();
            
            geomAdap = OraSpatialManager.getGeometryAdapter("SDO", "9", STRUCT.class, null, null, mainCon);
            mainResultset.last();
            
                xy = new double[mainResultset.getRow()][2];
              mainResultset.beforeFirst();
              
              while (mainResultset.next()) 
              {
                  shape =(STRUCT) mainResultset.getObject("hydrantLoc");
                  geo =geomAdap.importGeometry(shape);
                   if(geo instanceof oracle.sdoapi.geom.Point)
                   {
                        oracle.sdoapi.geom.Point point = (oracle.sdoapi.geom.Point) geo;
                        xy[mainResultset.getRow() - 1][0] = point.getX();
                        xy[mainResultset.getRow() - 1][1] = point.getY();
                      
                    }
              }
               g.setColor(Color.GREEN);
               for (int i = 0; i < xy.length; i++) 
               {
                      g.fillRect((int) xy[i][0] - 5, (int) xy[i][1] - 5, 15, 15);
               }
            
            
        } catch (SQLException ex) {
            Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidGeometryException ex) {
            Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GeometryInputTypeNotSupportedException ex) {
            Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
        }
                
            }
         
      public void getquery3()
      {
          getfirebuilding();
          
          CoordPoint xy[][];
        Graphics2D g = (Graphics2D)jLabel1.getGraphics();
        try {
            
          query = "SELECT b1.buildingName,b1.buildingShape FROM building b1, building b2, firebuilding f WHERE"
                  +" b2.buildingName = f.buildingName AND b1.buildingName<>f.buildingName"
                  +" AND MDSYS.SDO_WITHIN_DISTANCE(b1.buildingShape,b2.buildingShape,'distance=100')"
                  +" = 'TRUE'";
          mainResultset =mainStat.executeQuery(query);
          
          meta = mainResultset.getMetaData();

          geomAdap = OraSpatialManager.getGeometryAdapter("SDO", "9", STRUCT.class, null, null, mainCon);

              
                mainResultset.last();
              xy= new CoordPoint[mainResultset.getRow()][];
              mainResultset.beforeFirst();
              
              while(mainResultset.next()) 
              {
                    shape= (STRUCT) mainResultset.getObject("buildingShape");
                try {
                    geo =geomAdap.importGeometry(shape);
                } catch (InvalidGeometryException ex) {
                    Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
                } catch (GeometryInputTypeNotSupportedException ex) {
                    Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
                }
                    if (geo instanceof oracle.sdoapi.geom.Polygon)
                    {
                        oracle.sdoapi.geom.Polygon polygon = (oracle.sdoapi.geom.Polygon) geo;

                        xy[mainResultset.getRow() - 1] = polygon.getRingAt(0).getPointArray();
                    }
                }
              g.setColor(Color.YELLOW);
                int j;
                for (int i = 0; i < xy.length; i++) {
                    for (j = 0; j < xy[i].length - 1; j++) {
                        g.drawLine((int) xy[i][j].getX(), (int) xy[i][j].getY(), (int) xy[i][j + 1].getX(), (int) xy[i][j + 1].getY());
                    }
                } 
              
        } catch (SQLException ex) {
            Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
        }
              
          
      }
            
      public void getquery4()
        {
//            jLabel1.repaint();
               Graphics2D g = (Graphics2D)jLabel1.getGraphics();
               
        for(int p=0; p<npoints ; p++)
       {
               CoordPoint xy[][]=null;
               
        try {
             
             query1= "SELECT b.buildingShape FROM building b"
                     + " WHERE "
                     + "SDO_ANYINTERACT(b.buildingShape,"
                     +"SDO_GEOMETRY(2001, NULL,SDO_POINT_TYPE( "
                     +points[p].x+","+points[p].y+",NULL),NULL,NULL)) = 'TRUE'";
             
             
             
              mainResultset =mainStat.executeQuery(query1);
              
              
          if(mainResultset.next() == true)
          {
          
          meta = mainResultset.getMetaData();

          geomAdap = OraSpatialManager.getGeometryAdapter("SDO", "9", STRUCT.class, null, null, mainCon);

              
                mainResultset.last();
              xy= new CoordPoint[mainResultset.getRow()][];
              mainResultset.beforeFirst();
              
              while(mainResultset.next()) 
              {
                    shape= (STRUCT) mainResultset.getObject("buildingShape");
                try {
                    geo =geomAdap.importGeometry(shape);
                } catch (InvalidGeometryException ex) {
                    Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
                } catch (GeometryInputTypeNotSupportedException ex) {
                    Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
                }
                    if (geo instanceof oracle.sdoapi.geom.Polygon)
                    {
                        oracle.sdoapi.geom.Polygon polygon = (oracle.sdoapi.geom.Polygon) geo;

                        xy[mainResultset.getRow() - 1] = polygon.getRingAt(0).getPointArray();
                    }
                }
              g.setColor(Color.RED);
              g.setStroke(new BasicStroke(3));
                int j;
                for (int i = 0; i < xy.length; i++) {
                    for (j = 0; j < xy[i].length - 1; j++) {
                        g.drawLine((int) xy[i][j].getX(), (int) xy[i][j].getY(), (int) xy[i][j + 1].getX(), (int) xy[i][j + 1].getY());
                    }
                } 
                
//             query = "";
             
             double xy2[][];
             
             
              query = "SELECT * FROM hydrant h "
                    + "WHERE SDO_NN(h.hydrantLoc,("+query1+"),'sdo_num_res=1') = 'TRUE'";
              
              mainResultset = mainStat.executeQuery(query);
            meta = mainResultset.getMetaData();
            
            geomAdap = OraSpatialManager.getGeometryAdapter("SDO", "9", STRUCT.class, null, null, mainCon);
            mainResultset.last();
            
                xy2 = new double[mainResultset.getRow()][2];
              mainResultset.beforeFirst();
              
              while (mainResultset.next()) 
              {
                  shape =(STRUCT) mainResultset.getObject("hydrantLoc");
                  geo =geomAdap.importGeometry(shape);
                   if(geo instanceof oracle.sdoapi.geom.Point)
                   {
                        oracle.sdoapi.geom.Point point = (oracle.sdoapi.geom.Point) geo;
                        xy2[mainResultset.getRow() - 1][0] = point.getX();
                        xy2[mainResultset.getRow() - 1][1] = point.getY();
                      
                    }
              }
               g.setColor(Color.GREEN);
               for (int i = 0; i < xy2.length; i++) 
               {
                      g.fillRect((int) xy2[i][0] - 5, (int) xy2[i][1] - 5, 15, 15);
               }
                retrieve_query(queryNo++);
                setQuery();
                
          }
          else
          {
              mTextArea1.setText("("+points[p].x+","+points[p].y+")"+"point is an Invalid point");
          }
              
        
       }catch (SQLException ex) {
            Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidGeometryException ex) {
            Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GeometryInputTypeNotSupportedException ex) {
            Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
        }
        }
        }
                        
            
            
         public void retrieve_query(int queryNo) {
             
              query = "Query " + queryNo + ": " + query +";\n\n";
         }
         
         private void setQuery()
         {
            mTextArea1.setText(query);
             query = "";
    }
         
         public void resetall()
         {
            for (int i = 0; i < npoints - 1; i++) {
            points[i] = null;
        }
            npoints = 0;
            selectregion = false;
            selectpoint = false;
            mouseClicked = false;
            done = false;
            jLabel1.repaint();
         }
          
    
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
                    System.exit(-1);
            }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        
         try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(hw2.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(hw2.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(hw2.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(hw2.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new hw2().setVisible(true);
            }
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JCheckBox jCheckBox3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JRadioButton mRadioButton1;
    private javax.swing.JRadioButton mRadioButton2;
    private javax.swing.JRadioButton mRadioButton3;
    private javax.swing.JRadioButton mRadioButton4;
    private javax.swing.JTextArea mTextArea1;
    private javax.swing.ButtonGroup mbuttonGroup1;
    // End of variables declaration//GEN-END:variables

   
}

