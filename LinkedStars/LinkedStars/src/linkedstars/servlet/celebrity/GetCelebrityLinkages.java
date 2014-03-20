package linkedstars.servlet.celebrity;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import linkedstars.model.celebrity.PeopleAttributes;
import linkedstars.model.celebrity.FilmAttributes;

/**
 * Servlet implementation class GetCelebrityLinkages
 */
@WebServlet("/GetCelebrityLinkages")
public class GetCelebrityLinkages extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetCelebrityLinkages() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub

		String celebrity1 = request.getParameter("celebrity1");
		String celebrity2 = request.getParameter("celebrity2");
		int depth 		  = new Integer(request.getParameter("depth"));
		
		System.out.println("Fetch Linkage of Celebrities: "+celebrity1+","+celebrity2);
		
		try 
		{
			JSONObject peopleAttributes = new PeopleAttributes().getAllPeopleAttributes(celebrity1,celebrity2,depth);
			JSONObject filmAttributes 	= new FilmAttributes().getAllFilmAttributes(celebrity1, celebrity2, depth);
			
			JSONObject linkage = new JSONObject();
		
			linkage.put("name", "Link");
			linkage.put("children", new JSONArray().put(peopleAttributes).put(filmAttributes));
			
			response.setContentType("application/json");
			
			PrintWriter out = response.getWriter();
			out.print(linkage);
		} 
		catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
