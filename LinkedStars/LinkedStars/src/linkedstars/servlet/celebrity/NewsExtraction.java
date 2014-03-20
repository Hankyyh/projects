package linkedstar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Servlet implementation class NewsExtraction
 */
@WebServlet("/NewsExtraction")
public class NewsExtraction extends HttpServlet {
	PrintWriter out = null; 
	private static final long serialVersionUID = 1L;

    public NewsExtraction() {
        super();
    }


	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	 
		out = response.getWriter();
//		    out.println("Hello");
		    
		    
		    String first = request.getParameter("first_name").toLowerCase().replace(" ", "+");
			String second= request.getParameter("second_name").toLowerCase().replace(" ", "+");
//			out.println(first);
//			out.println(second);
			googleNews("http://news.google.com/news?q="+first+","+second+"&output=rss");
			xmlparser(request.getParameter("first_name").trim(),request.getParameter("second_name").trim());
			downloadPage("http://api.nytimes.com/svc/search/v2/articlesearch.json?q="+first+","+second+"&facet_field=source&api-key=aef5af09698d31ab7ede0043560c8098:18:68426105");
			data(request.getParameter("first_name").trim(),request.getParameter("second_name").trim());

	}

	public void downloadPage(String url)
    {
        try
        {
        	PrintWriter writer = null;
        	InputStream is = null;
            BufferedReader br;
            String line;
            URL pageURL = new URL(url);
            File out = new File("news.json");
            writer = new PrintWriter(new BufferedWriter(new FileWriter(out)));
            is = pageURL.openStream();  // throws an IOException
            br = new BufferedReader(new InputStreamReader(is));
            try {
            	 while ((line = br.readLine()) != null) {
                    writer.println(line);
                }
            }
            finally{
            	 if (is != null) is.close();
                if (writer != null) try {writer.close();} catch (Exception ignored) {}
                
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }
//	
	public void data(String first,String second){
		JSONParser parser = new JSONParser();
		ArrayList<News> array = new ArrayList<News>();
		String[] firstName = first.split(" ");
		String[] secondName = second.split(" ");
		boolean f = false;
		boolean s = false;
		String headline;
		String link;
		String pub_date;
		String desc = null;
		  try {

		        Object obj = parser.parse(new FileReader("news.json"));

		        JSONObject jsonObject =  (JSONObject) obj;
		        JSONObject j = (JSONObject) jsonObject.get("response");
		        
		        // loop array
		        JSONArray docs = (JSONArray) j.get("docs");
		      Iterator news = docs.iterator();
		      while(news.hasNext()){
		    	  JSONObject je = (JSONObject) news.next();
		    	  JSONObject headlines = (JSONObject) je.get("headline");
		    	  headline = headlines.get("main").toString();
		    	  link = je.get("web_url").toString();
		    	  link = "<a href='"+link+"'>"+headline+"</a>";
		    	  pub_date = je.get("pub_date").toString();
		    	  desc =(String)je.get("abstract");
//		    	  System.out.println("Headline :"+headline);
		    	  System.out.println("Web Url :"+je.get("web_url"));
//		    	  System.out.println("Pub Date :"+je.get("pub_date"));
//		    	  System.out.println("Abstract :"+je.get("abstract")); 
		    	  
		    	  JSONArray keywords = (JSONArray) je.get("keywords");
		    	  Iterator keys = keywords.iterator();
		    	  while(keys.hasNext()){
		    		  JSONObject key = (JSONObject) keys.next();
		    		  if(key.get("name").equals("persons")){
		    			  String name = key.get("value").toString();
		    			  if(name.contains(firstName[0])&& name.contains(firstName[1])){
		    				  f = true;
		    			  }
		    			  if(name.contains(secondName[0])&& name.contains(secondName[1])){
		    				  s = true;
		    			  }
//		    			  System.out.println("Person name: "+name);	
		    			  
		    		  }
		    		  
		    	  }
		    	  if(f && s){
//		    		  System.out.println("**********"+f+s+firstName[0]+firstName[1]+secondName[0]+secondName[1]);
		    		  array.add(new News(headline,link,pub_date,desc));
		    		  f=false;
		    		  s=false;
		    	  }
//		    		System.out.println("=============");	  
		      }
		       

		    } catch (FileNotFoundException e) {
		        e.printStackTrace();
		    } catch (IOException e) {
		        e.printStackTrace();
		    } catch (org.json.simple.parser.ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		  for(News n:array){
			 
				out.println("<tr><td>");
//				out.println("<b>"+n.headline+"</b>");
				out.println("<b>"+n.link+"</b>");
//				out.println(n.desc);
				out.println("<p>"+n.pub_date+"</p>");
				out.println("</td></tr>");
			}
	}
//	
	public void googleNews(String url){
		try
        {
			System.out.println(url);
        	PrintWriter writer = null;
        	InputStream is = null;
            BufferedReader br;
            String line;
            URL pageURL = new URL(url);
            File out = new File("news.xml");
            writer = new PrintWriter(new BufferedWriter(new FileWriter(out)));
            is = pageURL.openStream();  // throws an IOException
            br = new BufferedReader(new InputStreamReader(is));
            try {
            	 while ((line = br.readLine()) != null) {
                    writer.println(line);
                }
            }
            finally{
            	 if (is != null) is.close();
                if (writer != null) try {writer.close();} catch (Exception ignored) {}
                
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
	}
	
	
	public void xmlparser(String first,String second){
		File fXmlFile = new File("news.xml");
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		ArrayList<News> array = new ArrayList<News>();
		
		try {
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();
			System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
			
			NodeList nList = doc.getElementsByTagName("item");
			
			for (int temp = 0; temp < nList.getLength(); temp++) {
				 
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					 
					Element eElement = (Element) nNode;
					String headline = eElement.getElementsByTagName("title").item(0).getFirstChild().toString().replace("[#text:", "").replace("]", "");
					String link =eElement.getElementsByTagName("link").item(0).getFirstChild().toString().replace("[#text:", "").replace("]", "").replaceAll("(.*)(&url=)", "<a href='")+"' >"+headline+"</a>";
					String pub_date = eElement.getElementsByTagName("pubDate").item(0).getFirstChild().toString().replace("[#text:", "").replace("]", "");
					String desc = eElement.getElementsByTagName("description").item(0).getFirstChild().toString().replace("[#text:", "").replace("]", "");
//					String link =eElement.getElementsByTagName("link").item(0).getTextContent();
//					String pub_date = eElement.getElementsByTagName("pubDate").item(0).getTextContent();
//					String desc = eElement.getElementsByTagName("description").item(0).getTextContent();
					
					if(headline.contains(first.replace("+", " ")) && headline.contains(second.replace("+", " "))){
						array.add(new News(headline,link,pub_date,desc));
					}
//					System.out.println("Title : " + eElement.getElementsByTagName("title").item(0).getTextContent());
					System.out.println("Link : " + link);
//					System.out.println("Pub Date : " + eElement.getElementsByTagName("pubDate").item(0).getTextContent());
//					System.out.println("Description : " + eElement.getElementsByTagName("description").item(0).getTextContent());
//					System.out.println("===============");
				}
			}
			
		} catch (SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(News n:array){
			out.println("<tr><td>");
//			out.println("<b>"+n.headline+"</b>");
			out.println("<b>"+n.link+"</b>");
//			out.println(n.desc);
			out.println("<p>"+n.pub_date+"</p>");
			out.println("</td></tr>");
			
		}
	 
	}
	



}


class News{
	String headline;
	String link;
	String pub_date;
	String desc;
	News(String headline,String link,String pub_date,String desc){
		this.headline = headline;
		this.link = link;
		this.pub_date = pub_date;
		this.desc = desc;
	}

	public  void print(String headline,String link,String pub_date,String desc){
		System.out.println("Title : " + headline);
		System.out.println("Link : " + link);
		System.out.println("Pub Date : " + pub_date);
		System.out.println("Description : " + desc);
		System.out.println("===============**********==============");
	}

}

