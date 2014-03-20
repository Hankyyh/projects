/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author vineet
 */

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.json.JSONObject;

import javax.xml.parsers.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;



public class HelloWorldExample extends HttpServlet{
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        
        PrintWriter out = null;
        try
        {
            out = response.getWriter();
            request.setCharacterEncoding("UTF-8");
            response.setContentType("text/html; charset=UTF-8");
            Enumeration e = request.getParameterNames();
            String input = "";
            input = (String)e.nextElement();

            String value1 = request.getParameter(input);
            value1 = URLEncoder.encode(value1, "utf-8");
            

            
            String type = (String)e.nextElement();
            String value2 = request.getParameter(type);
            
            
            String urlString = "http://cs-server.usc.edu:23824/cgi-bin/get_Discography.php?" + input + "=" + value1 + "&" + type + "=" + value2;
            
            
            URL url = new URL(urlString);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setAllowUserInteraction(false);
            InputStream urlStream = url.openStream();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(true);
            DocumentBuilder db;
            Document doc = null;
            db = dbf.newDocumentBuilder();
            
            doc = db.parse(urlStream);
            
            doc.getDocumentElement().normalize();
            
            NodeList nodes = doc.getElementsByTagName("result");
            
//            out.println(nodes.getLength());
            
            int value;
            if(value2.equals("artists"))
            {
                value = 1;
            }
            else if(value2.equals("albums"))
            {
                value = 2;
            }
            else
            {
                value = 3;
            }
            String str_json = "";
            switch(value)
            {
                case 1:
                    
                    
                    if(nodes.getLength() == 0)
                    {
                        str_json = "{\"results\":\"No Result found\"}";
                    }
                    else
                    {
                        JSONObject results = new JSONObject();
                        JSONObject result = new JSONObject();
                        
                        JSONObject attrr = new JSONObject();    //to fill the first element
                        result.accumulate("result",attrr);
                        
                        for(int i =0; i<nodes.getLength(); i++)     //check if -1 is require
                        {
                            JSONObject attr = new JSONObject();
                            Element read = (Element)nodes.item(i);
                            String image = read.getAttribute("cover");
                            String artist = read.getAttribute("name");
                            String genre = read.getAttribute("genre");
                            String year = read.getAttribute("year");
                            String detail = read.getAttribute("details");
                            
                            attr.put("cover",image);
                            attr.put("name",artist);
                            attr.put("genre",genre);
                            attr.put("year",year);
                            attr.put("details",detail);
                            result.accumulate("result",attr);
                            
                        }
                        results.put("results",result);
                        str_json = results.toString();
                    }
                    out.println(str_json);
                    break;
                    
                    
                case 2:
                    
                    
                    if(nodes.getLength() == 0)
                    {
                        str_json = "{\"results\":\"No Result found\"}";
                    }
                    else
                    {
                        JSONObject results = new JSONObject();
                        JSONObject result = new JSONObject();
                        
                        JSONObject attrr = new JSONObject();    //to fill the first element
                        result.accumulate("result",attrr);
                        
                        for(int i =0; i<nodes.getLength(); i++)     //check if -1 is require
                        {
                            JSONObject attr = new JSONObject();
                            Element read = (Element)nodes.item(i);
                            String image = read.getAttribute("cover");
                            String title = read.getAttribute("title");
                            String artist = read.getAttribute("artist");
                            String genre = read.getAttribute("genre");
                            String year = read.getAttribute("year");
                            String detail = read.getAttribute("detail");
                            
                            attr.put("cover",image);
                            attr.put("title",title);
                            attr.put("artist",artist);
                            attr.put("genre",genre);
                            attr.put("year",year);
                            attr.put("detail",detail);
                            result.accumulate("result",attr);
                            
                        }
                        results.put("results",result);
                        str_json = results.toString();
                    }
                    out.println(str_json);
                    break;
                    
                    
                    
                case 3:
                    
                    
                    if(nodes.getLength() == 0)
                    {
                        str_json = "{\"results\":\"No Result found\"}";
                    }
                    else
                    {
                        JSONObject results = new JSONObject();
                        JSONObject result = new JSONObject();
                        
                        JSONObject attrr = new JSONObject();    //to fill the first element
                        result.accumulate("result",attrr);
                        for(int i =0; i<nodes.getLength(); i++)     //check if -1 is require
                        {
                            JSONObject attr = new JSONObject();
                            Element read = (Element)nodes.item(i);
                            String sample = read.getAttribute("sample");
                            String title = read.getAttribute("title");
                            String performer = read.getAttribute("performer");
                            String composer = read.getAttribute("composer");
                            String detail = read.getAttribute("detail");
                            
                            attr.put("sample",sample);
                            attr.put("title",title);
                            attr.put("performer",performer);
                            attr.put("composer",composer);
                            attr.put("detail",detail);
                            result.accumulate("result",attr);
                            
                        }
                        results.put("results",result);
                        str_json = results.toString();
                    }
                    out.println(str_json);
                    break;
                    
                    
                    
                    
                    
            }
            
        }
        catch(MalformedURLException ex)
		{
			out.println("{\"error\":{\"message\": \"Malformed URL Exception Encountered\"}}");
			throw new IOException();
		}
		catch(IOException ioe)
		{
			out.println("{\"error\":{\"message\": \"An IO Exception Encountered\"}}");
			throw new IOException();
		}
        catch(Exception e)
		{
            out.println(e.toString());
			out.println("1");
		}
		finally
		{
			out.close();
		}
        
    }
}
