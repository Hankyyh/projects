package linkedstars.model.celebrity;

import org.json.JSONException;
import org.json.JSONObject;

public class jsonTest 
{
	public static void main(String[] args) throws JSONException 
	{
		// TODO Auto-generated method stub
		
		JSONObject json = new JSONObject();
		
		JSONObject celebrity = new JSONObject();
		celebrity.put("name", "Amy Adams");
		celebrity.put("image", "http://amyadams.org");
		
		json.accumulate("celebrity", celebrity);

		celebrity = new JSONObject();
		celebrity.put("name", "Ben Affleck");
		celebrity.put("image", "http://benaffleck.org");
		
		json.accumulate("celebrity", celebrity);
		
		System.out.println(json);
	}

}
