package linkedstars.model.celebrity;

import linkedstars.dataaccess.repository.RepositoryManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;

public class FilmAttributes 
{
	public static String stripQuotes(Value value)
	{
		if(value!=null)
			return value.toString().replace("\"","");
		else
			return null;
	}
	
	public JSONObject getAllFilmAttributes(String celebrity1, String celebrity2, int depth)
	{
		String query1 = "PREFIX ls: <http://linkedstars.org/> SELECT ?filmName ?worked1 ?character1 ?characternote1 ?specialperformancetype1 ?language1 ?appearance1 ?worked2 ?character2 ?characternote2 ?specialperformancetype2 ?language2 ?appearance2 WHERE { ?x ls:name \""+celebrity1+"\". ?y ls:name \""+celebrity2+"\". { ?x ?worked1 ?workedType1. ?workedType1 <http://linkedstars.org/film/id> ?film. ?y ?worked2 ?workedType2. ?workedType2 <http://linkedstars.org/film/id> ?film. ?film <http://linkedstars.org/film/name> ?filmName. OPTIONAL { ?workedType1 <http://linkedstars.org/film/character> ?character1}. OPTIONAL { ?workedType1 <http://linkedstars.org/film/characternote> ?characternote1}. OPTIONAL { ?workedType1 <http://linkedstars.org/film/specialperformancetype> ?specialperformancetype1}. OPTIONAL { ?workedType1 <http://linkedstars.org/film/language> ?language1}. OPTIONAL { ?workedType1 <http://linkedstars.org/film/appearancetype> ?appearance1}. OPTIONAL { ?workedType2 <http://linkedstars.org/film/character> ?character2}. OPTIONAL { ?workedType2 <http://linkedstars.org/film/characternote> ?characternote2}. OPTIONAL { ?workedType2 <http://linkedstars.org/film/specialperformancetype> ?specialperformancetype2}. OPTIONAL { ?workedType2 <http://linkedstars.org/film/language> ?language2}. OPTIONAL { ?workedType2 <http://linkedstars.org/film/appearancetype> ?appearance2}. } UNION { ?x ?worked1 ?workedType1. ?workedType1 <http://linkedstars.org/film/id> ?film. ?y ?worked2 ?film. ?film <http://linkedstars.org/film/name> ?filmName. OPTIONAL { ?workedType1 <http://linkedstars.org/film/character> ?character1}. OPTIONAL { ?workedType1 <http://linkedstars.org/film/characternote> ?characternote1}. OPTIONAL { ?workedType1 <http://linkedstars.org/film/specialperformancetype> ?specialperformancetype1}. OPTIONAL { ?workedType1 <http://linkedstars.org/film/language> ?language1}. OPTIONAL { ?workedType1 <http://linkedstars.org/film/appearancetype> ?appearance1}. } UNION { ?x ?worked1 ?film. ?y ?worked2 ?workedType2. ?workedType2 <http://linkedstars.org/film/id> ?film. ?film <http://linkedstars.org/film/name> ?filmName. OPTIONAL { ?workedType2 <http://linkedstars.org/film/character> ?character2}. OPTIONAL { ?workedType2 <http://linkedstars.org/film/characternote> ?characternote2}. OPTIONAL { ?workedType2 <http://linkedstars.org/film/specialperformancetype> ?specialperformancetype2}. OPTIONAL { ?workedType2 <http://linkedstars.org/film/language> ?language2}. OPTIONAL { ?workedType2 <http://linkedstars.org/film/appearancetype> ?appearance2}. } } ORDER BY ?filmName";		
		String query = null;

        //ACCESS REPOSITORY
		RepositoryManager repoManager = new RepositoryManager();
		RepositoryConnection con = repoManager.getConnection("LinkedStars");
       
        //QUERY AND WRITE TO FILE
        try 
        {
    	   switch(depth)
    	   {
    	   	   case 1:
    	   		   query = query1;
    	   		   break;
    		   default:
    			   query = query1;
    	   		   break;
    	   }
    	
           TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
           TupleQueryResult result = tupleQuery.evaluate();
            	
           JSONObject celebrityFilmAttributes = new JSONObject();
           JSONArray filmArray = new JSONArray();
           
           String prevFilm="";
           JSONArray workProfiles = new JSONArray();
           
           if(result.hasNext())
           {
	           BindingSet bindingSet = result.next();
	           
	           String film = stripQuotes(bindingSet.getValue("filmName"));
	           String worked1 = stripQuotes(bindingSet.getValue("worked1"));
	           String character1 = stripQuotes(bindingSet.getValue("character1"));
	           String characternote1 = stripQuotes(bindingSet.getValue("characternote1"));
	           String specialperformancetype1 = stripQuotes(bindingSet.getValue("specialperformancetype1"));
	           String language1 = stripQuotes(bindingSet.getValue("language1"));
	           String appearance1 = stripQuotes(bindingSet.getValue("appearance1"));
	           
	           String worked2 = stripQuotes(bindingSet.getValue("worked2"));
	           String character2 = stripQuotes(bindingSet.getValue("character2"));
	           String characternote2 = stripQuotes(bindingSet.getValue("characternote2"));
	           String specialperformancetype2 = stripQuotes(bindingSet.getValue("specialperformancetype2"));
	           String language2 = stripQuotes(bindingSet.getValue("language2"));
	           String appearance2 = stripQuotes(bindingSet.getValue("appearance2"));

	           prevFilm = film;
	           
	           if(film!=null)
	           {                	
	           	String work1 = "";
	           	String work2 = "";
	           	JSONArray work1Details =  new JSONArray();
	           	JSONArray work2Details =  new JSONArray();
	           	
	           	if(worked1!=null)
	           	{
	           		if(worked1.equals("http://linkedstars.org/acted/id"))
	           		{
	           			work1="Actor";
	           			JSONArray actorAttributes = new JSONArray();
	           			if(character1!=null)
	           				actorAttributes.put(new JSONObject().put("name", "Character: "+character1));
	           			if(characternote1!=null)
	           				actorAttributes.put(new JSONObject().put("name", "CharacterNote: "+characternote1));
	           			if(characternote1!=null)
	           				actorAttributes.put(new JSONObject().put("name", "SpclPerformanceType: "+specialperformancetype1));
	           			
	           			JSONObject celebrityActor = new JSONObject();
	           			celebrityActor.put("name", celebrity1);
	           			
	           			if(actorAttributes.length()>0)
	           				celebrityActor.put("children", actorAttributes);
	           			
	           			work1Details.put(celebrityActor);
	           		}
	           		else if(worked1.equals("http://linkedstars.org/dubbed/id"))
	           		{
	           			work1="DubbedArtist";
	           			JSONArray actorAttributes = new JSONArray();
	           			if(character1!=null)
	           				actorAttributes.put(new JSONObject().put("name", "Character: "+character1));
	           			if(characternote1!=null)
	           				actorAttributes.put(new JSONObject().put("name", "DubbedLanguage: "+language1));
	           			
	           			JSONObject celebrityActor = new JSONObject();
	           			celebrityActor.put("name", celebrity1);
	
	           			if(actorAttributes.length()>0)
	           				celebrityActor.put("children", actorAttributes);
	           			
	           			work1Details.put(celebrityActor);
	           		}
	           		else if(worked1.equals("http://linkedstars.org/appeared/id"))
	           		{
	           			work1="Appearance";
	           			JSONArray actorAttributes = new JSONArray();
	           			if(appearance1!=null)
	           				actorAttributes.put(new JSONObject().put("name", "Appearance: "+appearance1));
	           			
	           			JSONObject celebrityActor = new JSONObject();
	           			celebrityActor.put("name", celebrity1);
	           			
	           			if(actorAttributes.length()>0)
	           				celebrityActor.put("children", actorAttributes);
	           			
	           			work1Details.put(celebrityActor);
	           		}
	           		else
	           		{
	           			switch(worked1)
	           			{
	           				case "http://linkedstars.org/directed/film":
	           					work1="Director"; break;
	           				case "http://linkedstars.org/writing/film":
	           					work1="Writer"; break;
	           				case "http://linkedstars.org/executiveproduced/film":
	           					work1="Executive Producer"; break;
	           				case "http://linkedstars.org/produced/film":
	           					work1="Producer"; break;
	           				case "http://linkedstars.org/story/film":
	           					work1="Story Credits"; break;
	           				case "http://linkedstars.org/cinematography/film":
	           					work1="Cinematographer"; break;
	           			}
	           			
	           			JSONObject celebrityWorker = new JSONObject();
	           			celebrityWorker.put("name", celebrity1);
	           			
	           			work1Details.put(celebrityWorker);
	           		}
	           	}
	           	if(worked2!=null)
	           	{
	           		if(worked2.equals("http://linkedstars.org/acted/id"))
	           		{
	           			work2="Actor";
	           			JSONArray actorAttributes = new JSONArray();
	           			if(character2!=null)
	           				actorAttributes.put(new JSONObject().put("name", "Character: "+character2));
	           			if(characternote2!=null)
	           				actorAttributes.put(new JSONObject().put("name", "CharacterNote: "+characternote2));
	           			if(characternote2!=null)
	           				actorAttributes.put(new JSONObject().put("name", "SpclPerformanceType: "+specialperformancetype2));
	           			
	           			JSONObject celebrityActor = new JSONObject();
	           			celebrityActor.put("name", celebrity2);
	           			
	           			if(actorAttributes.length()>0)
	           				celebrityActor.put("children", actorAttributes);
	           			
	           			work2Details.put(celebrityActor);
	           		}
	           		else if(worked2.equals("http://linkedstars.org/dubbed/id"))
	           		{
	           			work2="DubbedArtist";
	           			JSONArray actorAttributes = new JSONArray();
	           			if(character2!=null)
	           				actorAttributes.put(new JSONObject().put("name", "Character: "+character2));
	           			if(characternote2!=null)
	           				actorAttributes.put(new JSONObject().put("name", "DubbedLanguage: "+language2));
	           			
	           			JSONObject celebrityActor = new JSONObject();
	           			celebrityActor.put("name", celebrity2);
	           			
	           			if(actorAttributes.length()>0)
	           				celebrityActor.put("children", actorAttributes);
	           			
	           			work2Details.put(celebrityActor);
	           		}
	           		else if(worked2.equals("http://linkedstars.org/appeared/id"))
	           		{
	           			work2="Appearance";
	           			JSONArray actorAttributes = new JSONArray();
	           			if(appearance2!=null)
	           				actorAttributes.put(new JSONObject().put("name", "Appearance: "+appearance2));
	           			
	           			JSONObject celebrityActor = new JSONObject();
	           			celebrityActor.put("name", celebrity2);
	           			
	           			if(actorAttributes.length()>0)
	           				celebrityActor.put("children", actorAttributes);
	           			
	           			work2Details.put(celebrityActor);
	           		}
	           		else
	           		{
	           			switch(worked2)
	           			{
	           				case "http://linkedstars.org/directed/film":
	           					work2="Director"; break;
	           				case "http://linkedstars.org/writing/film":
	           					work2="Writer"; break;
	           				case "http://linkedstars.org/executiveproduced/film":
	           					work2="Executive Producer"; break;
	           				case "http://linkedstars.org/produced/film":
	           					work2="Producer"; break;
	           				case "http://linkedstars.org/story/film":
	           					work2="Story Credits"; break;
	           				case "http://linkedstars.org/cinematography/film":
	           					work2="Cinematographer"; break;
	           			}
	           			
	           			JSONObject celebrityWorker = new JSONObject();
	           			celebrityWorker.put("name", celebrity2);
	           			
	           			work2Details.put(celebrityWorker);
	           		}
	           	}
	           	
	           	if(work1Details.length()>0 && work2Details.length()>0)
	           	{
	           		if(work1.equals(work2))
	           		{
	           			JSONArray workDetails = new JSONArray();
	           			
	           			for(int i=0; i<work1Details.length(); i++)
	           				workDetails.put(work1Details.get(i));
	           			
	           			for(int j=0; j<work2Details.length();j++)
	           				workDetails.put(work2Details.get(j));
	           			
	           			JSONObject workProfile = new JSONObject();
	           			workProfile.put("name",work1);
	           			workProfile.put("children", workDetails);
	           			workProfiles.put(workProfile);
	           		}
	           		else
	           		{
	           			JSONObject workProfile = new JSONObject();
	           			workProfile.put("name",work1);
	           			workProfile.put("children", work1Details);
	           			workProfiles.put(workProfile);
	           			
	           			workProfile = new JSONObject();
	           			workProfile.put("name",work2);
	           			workProfile.put("children", work2Details);
	           			workProfiles.put(workProfile);
	           		}
	           	}
	           	else if(work1Details.length()>0)
	           	{
	           		JSONObject workProfile = new JSONObject();
	           		workProfile.put("name",work1);
	       			workProfile.put("children", work1Details);
	       			workProfiles.put(workProfile);
	           	}
	           	else if(work2Details.length()>0)
	           	{
	           		JSONObject workProfile = new JSONObject();
	           		workProfile.put("name",work2);
	       			workProfile.put("children", work2Details);
	       			workProfiles.put(workProfile);
	           	}

	           }
           }
           
           
           while(result.hasNext())
           {
                BindingSet bindingSet = result.next();
                
                String film = stripQuotes(bindingSet.getValue("filmName"))	;
                String worked1 = stripQuotes(bindingSet.getValue("worked1"));
                String character1 = stripQuotes(bindingSet.getValue("character1"));
                String characternote1 = stripQuotes(bindingSet.getValue("characternote1"));
                String specialperformancetype1 = stripQuotes(bindingSet.getValue("specialperformancetype1"));
                String language1 = stripQuotes(bindingSet.getValue("language1"));
                String appearance1 = stripQuotes(bindingSet.getValue("appearance1"));
                
                String worked2 = stripQuotes(bindingSet.getValue("worked2"));
                String character2 = stripQuotes(bindingSet.getValue("character2"));
                String characternote2 = stripQuotes(bindingSet.getValue("characternote2"));
                String specialperformancetype2 = stripQuotes(bindingSet.getValue("specialperformancetype2"));
                String language2 = stripQuotes(bindingSet.getValue("language2"));
                String appearance2 = stripQuotes(bindingSet.getValue("appearance2"));
                
                if(!film.equals(prevFilm))
                {	
    	           	filmArray.put(new JSONObject().put("name", prevFilm).put("children", workProfiles));
    	           	workProfiles = new JSONArray();
    	           	prevFilm = film;
                }
       	
            	String work1 = "";
            	String work2 = "";
            	JSONArray work1Details =  new JSONArray();
            	JSONArray work2Details =  new JSONArray();
            	
            	if(worked1!=null)
            	{
            		if(worked1.equals("http://linkedstars.org/acted/id"))
            		{
            			work1="Actor";
            			JSONArray actorAttributes = new JSONArray();
            			if(character1!=null)
            				actorAttributes.put(new JSONObject().put("name", "Character: "+character1));
            			if(characternote1!=null)
            				actorAttributes.put(new JSONObject().put("name", "CharacterNote: "+characternote1));
            			if(characternote1!=null)
            				actorAttributes.put(new JSONObject().put("name", "SpclPerformanceType: "+specialperformancetype1));
            			
            			JSONObject celebrityActor = new JSONObject();
            			celebrityActor.put("name", celebrity1);
            			
            			if(actorAttributes.length()>0)
            				celebrityActor.put("children", actorAttributes);
            			
            			work1Details.put(celebrityActor);
            		}
            		else if(worked1.equals("http://linkedstars.org/dubbed/id"))
            		{
            			work1="DubbedArtist";
            			JSONArray actorAttributes = new JSONArray();
            			if(character1!=null)
            				actorAttributes.put(new JSONObject().put("name", "Character: "+character1));
            			if(characternote1!=null)
            				actorAttributes.put(new JSONObject().put("name", "DubbedLanguage: "+language1));
            			
            			JSONObject celebrityActor = new JSONObject();
            			celebrityActor.put("name", celebrity1);

            			if(actorAttributes.length()>0)
            				celebrityActor.put("children", actorAttributes);
            			
            			work1Details.put(celebrityActor);
            		}
            		else if(worked1.equals("http://linkedstars.org/appeared/id"))
            		{
            			work1="Appearance";
            			JSONArray actorAttributes = new JSONArray();
            			if(appearance1!=null)
            				actorAttributes.put(new JSONObject().put("name", "Appearance: "+appearance1));
            			
            			JSONObject celebrityActor = new JSONObject();
            			celebrityActor.put("name", celebrity1);
            			
            			if(actorAttributes.length()>0)
            				celebrityActor.put("children", actorAttributes);
            			
            			work1Details.put(celebrityActor);
            		}
            		else
            		{
            			switch(worked1)
            			{
            				case "http://linkedstars.org/directed/film":
            					work1="Director"; break;
            				case "http://linkedstars.org/writing/film":
            					work1="Writer"; break;
            				case "http://linkedstars.org/executiveproduced/film":
            					work1="Executive Producer"; break;
            				case "http://linkedstars.org/produced/film":
            					work1="Producer"; break;
            				case "http://linkedstars.org/story/film":
            					work1="Story Credits"; break;
            				case "http://linkedstars.org/cinematography/film":
            					work1="Cinematographer"; break;
            			}
            			
            			JSONObject celebrityWorker = new JSONObject();
            			celebrityWorker.put("name", celebrity1);
            			
            			work1Details.put(celebrityWorker);
            		}
            	}
            	if(worked2!=null)
            	{
            		if(worked2.equals("http://linkedstars.org/acted/id"))
            		{
            			work2="Actor";
            			JSONArray actorAttributes = new JSONArray();
            			if(character2!=null)
            				actorAttributes.put(new JSONObject().put("name", "Character: "+character2));
            			if(characternote2!=null)
            				actorAttributes.put(new JSONObject().put("name", "CharacterNote: "+characternote2));
            			if(characternote2!=null)
            				actorAttributes.put(new JSONObject().put("name", "SpclPerformanceType: "+specialperformancetype2));
            			
            			JSONObject celebrityActor = new JSONObject();
            			celebrityActor.put("name", celebrity2);
            			
            			if(actorAttributes.length()>0)
            				celebrityActor.put("children", actorAttributes);
            			
            			work2Details.put(celebrityActor);
            		}
            		else if(worked2.equals("http://linkedstars.org/dubbed/id"))
            		{
            			work2="DubbedArtist";
            			JSONArray actorAttributes = new JSONArray();
            			if(character2!=null)
            				actorAttributes.put(new JSONObject().put("name", "Character: "+character2));
            			if(characternote2!=null)
            				actorAttributes.put(new JSONObject().put("name", "DubbedLanguage: "+language2));
            			
            			JSONObject celebrityActor = new JSONObject();
            			celebrityActor.put("name", celebrity2);
            			
            			if(actorAttributes.length()>0)
            				celebrityActor.put("children", actorAttributes);
            			
            			work2Details.put(celebrityActor);
            		}
            		else if(worked2.equals("http://linkedstars.org/appeared/id"))
            		{
            			work2="Appearance";
            			JSONArray actorAttributes = new JSONArray();
            			if(appearance2!=null)
            				actorAttributes.put(new JSONObject().put("name", "Appearance: "+appearance2));
            			
            			JSONObject celebrityActor = new JSONObject();
            			celebrityActor.put("name", celebrity2);
            			
            			if(actorAttributes.length()>0)
            				celebrityActor.put("children", actorAttributes);
            			
            			work2Details.put(celebrityActor);
            		}
            		else
            		{
            			switch(worked2)
            			{
            				case "http://linkedstars.org/directed/film":
            					work2="Director"; break;
            				case "http://linkedstars.org/writing/film":
            					work2="Writer"; break;
            				case "http://linkedstars.org/executiveproduced/film":
            					work2="Executive Producer"; break;
            				case "http://linkedstars.org/produced/film":
            					work2="Producer"; break;
            				case "http://linkedstars.org/story/film":
            					work2="Story Credits"; break;
            				case "http://linkedstars.org/cinematography/film":
            					work2="Cinematographer"; break;
            			}
            			
            			JSONObject celebrityWorker = new JSONObject();
            			celebrityWorker.put("name", celebrity2);
            			
            			work2Details.put(celebrityWorker);
            		}
            	}
            	
            	if(work1Details.length()>0 && work2Details.length()>0)
            	{
            		if(work1.equals(work2))
            		{
            			JSONArray workDetails = new JSONArray();
            			
            			for(int i=0; i<work1Details.length(); i++)
            				workDetails.put(work1Details.get(i));
            			
            			for(int j=0; j<work2Details.length();j++)
            				workDetails.put(work2Details.get(j));
            			
            			JSONObject workProfile = new JSONObject();
            			workProfile.put("name",work1);
            			workProfile.put("children", workDetails);
            			workProfiles.put(workProfile);
            		}
            		else
            		{
            			JSONObject workProfile = new JSONObject();
            			workProfile.put("name",work1);
            			workProfile.put("children", work1Details);
            			
            			boolean insertWork=true;
            			for(int i=0; i<workProfiles.length(); i++)
            			{
            				String checkProfile = workProfiles.getJSONObject(i).getString("name");

            				if(checkProfile.equals(work1))
            				{
            					insertWork=false;
            					break;
            				}
            			}
            			if(insertWork)
            				workProfiles.put(workProfile);
            			
            			workProfile = new JSONObject();
            			workProfile.put("name",work2);
            			workProfile.put("children", work2Details);
            			
            			insertWork=true;
            			for(int i=0; i<workProfiles.length(); i++)
            			{
            				String checkProfile = workProfiles.getJSONObject(i).getString("name");

            				if(checkProfile.equals(work2))
            				{
            					insertWork=false;
            					break;
            				}
            			}
            			if(insertWork)
            				workProfiles.put(workProfile);
            		}
            	}
            	else if(work1Details.length()>0)
            	{
            		JSONObject workProfile = new JSONObject();
            		workProfile.put("name",work1);
        			workProfile.put("children", work1Details);

        			boolean insertWork=true;
        			for(int i=0; i<workProfiles.length(); i++)
        			{
        				String checkProfile = workProfiles.getJSONObject(i).getString("name");

        				if(checkProfile.equals(work1))
        				{
        					insertWork=false;
        					break;
        				}
        			}
        			if(insertWork)
        				workProfiles.put(workProfile);
            	}
            	else if(work2Details.length()>0)
            	{
            		JSONObject workProfile = new JSONObject();
            		workProfile.put("name",work2);
        			workProfile.put("children", work2Details);
        			
        			boolean insertWork=true;
        			for(int i=0; i<workProfiles.length(); i++)
        			{
        				String checkProfile = workProfiles.getJSONObject(i).getString("name");

        				if(checkProfile.equals(work2))
        				{
        					insertWork=false;
        					break;
        				}
        			}
        			if(insertWork)
        				workProfiles.put(workProfile);
            	}
            }
           
           filmArray.put(new JSONObject().put("name", prevFilm).put("children", workProfiles));

           celebrityFilmAttributes.put("name", "CelebrityFilms");
           celebrityFilmAttributes.put("children", filmArray);
           
           result.close();
           con.close();
           
           return celebrityFilmAttributes;
         }
         catch (OpenRDFException e) {
            // handle exception
         }
		 catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		 }
        return null;
	}
}
