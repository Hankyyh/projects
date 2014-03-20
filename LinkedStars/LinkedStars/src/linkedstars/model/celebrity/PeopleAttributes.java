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


public class PeopleAttributes 
{
	public static String stripQuotes(Value value)
	{
		if(value!=null)
			return value.toString().replace("\"","");
		else
			return null;
	}
	
	public static String getImages()
	{
		// TODO Auto-generated method stub
		
		JSONObject json = new JSONObject();
		try 
        {
            //ACCESS REPOSITORY
			RepositoryManager repoManager = new RepositoryManager();
			RepositoryConnection con = repoManager.getConnection("LinkedStars");
           
            //QUERY AND WRITE TO FILE
            try 
            {
                   String queryString = "PREFIX ls: <http://linkedstars.org/> SELECT ?name ?image WHERE { ?x ls:name ?name. OPTIONAL { ?x <http://linkedstars.org/celebrity/image> ?image. } } ORDER BY ?y";
                   TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

                   TupleQueryResult result = tupleQuery.evaluate();
                   try 
                   {
                       System.out.println("\n\n\n");
                       
                       while(result.hasNext())
                       {
                            BindingSet bindingSet = result.next();
                            String name = stripQuotes(bindingSet.getValue("name"));
                            String image  = stripQuotes(bindingSet.getValue("image"));
                            
                            try 
                            {
                            	JSONObject celebrity = new JSONObject();
                        		celebrity.put("name", name);
                        		
                        		if(image!=null)
                        			celebrity.put("image", image);
                        		else
                        			celebrity.put("image", "NoImage");
                        			
                        		json.accumulate("celebrity", celebrity);
                        		
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
                            System.out.println(name+","+image);
                            System.out.println();
                       }
                       
                       System.out.println("\n\n\n");
                   }
                   finally 
                   {
                       result.close();
                   }
            }
            finally {
               con.close();
            }
         }
         catch (OpenRDFException e) {
            // handle exception
         } 
		return json.toString();
	}

	public String testPeopleAttributes(String celebrity1, String celebrity2, int depth)
	{
		String query1 = "PREFIX ls: <http://linkedstars.org/> SELECT * WHERE { ?x ls:name \""+celebrity1+"\". ?y ls:name \""+celebrity2+"\". { ?x ls:birthplace ?birthplace. ?y ls:birthplace ?birthplace. } UNION { ?x ls:nationality ?nationality. ?y ls:nationality ?nationality. } UNION { ?x ls:profession ?profession. ?y ls:profession ?profession. } }";		
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
            	
           JSONObject peopleAttributesArray = new JSONObject();
           peopleAttributesArray.put("name", "PeopleAttributes");
           
           JSONObject birthPlaceArray = new JSONObject();
           JSONObject nationalityArray = new JSONObject();
           JSONObject professionArray = new JSONObject();
           
           
           while(result.hasNext())
           {
                BindingSet bindingSet = result.next();
                
                JSONObject birthPlace = new JSONObject();
                JSONObject nationality = new JSONObject();
                JSONObject profession = new JSONObject();
                
                String birthPlaceValue = stripQuotes(bindingSet.getValue("birthplace"));
                String nationalityValue= stripQuotes(bindingSet.getValue("nationality"));
                String professionValue = stripQuotes(bindingSet.getValue("profession"));
                
                if(birthPlaceValue!=null)
                {
                	birthPlace.put("name", birthPlaceValue);
                    birthPlaceArray.accumulate("children", birthPlace);
                }
                if(nationalityValue!=null)
                {
                	nationality.put("name", nationalityValue);
                	nationalityArray.accumulate("children", nationality);
                }
                if(professionValue!=null)
                {
                	profession.put("name", professionValue);	
                	professionArray.accumulate("children", profession);
                } 
           }
           
           if(birthPlaceArray.has("children"))
           {
        	   birthPlaceArray.put("name", "Birth Place");
        	   peopleAttributesArray.accumulate("children", birthPlaceArray);
           }
           if(nationalityArray.length()>0)
           {
        	   nationalityArray.put("name", "Nationality");
        	   peopleAttributesArray.accumulate("children", nationalityArray);
           }
           if(professionArray.length()>0)
           {
        	   professionArray.put("name", "Profession");
        	   peopleAttributesArray.accumulate("children", professionArray);
           }
           
           result.close();
           con.close();
           
           return peopleAttributesArray.toString();
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
	
	public JSONObject getAllPeopleAttributes(String celebrity1, String celebrity2, int depth)
	{
		String query1 = "PREFIX ls: <http://linkedstars.org/> SELECT ?birthplace ?nationality ?profession ?religion ?ethnicity ?height ?weight ?language ?parent ?child ?sibling ?spouse ?institute ?degreeX ?majorX ?inststartX ?instendX ?specialX ?degreeY ?majorY ?inststartY ?instendY ?specialY ?employer ?emptitleX ?empstartX ?empendX ?emptitleY ?empstartY ?empendY ?location ?locstartX ?locendX ?locstartY ?locendY WHERE { ?x ls:name \""+celebrity1+"\". ?y ls:name \""+celebrity2+"\". { ?x ls:birthplace ?birthplace. ?y ls:birthplace ?birthplace. } UNION { ?x ls:nationality ?nationality. ?y ls:nationality ?nationality. } UNION { ?x ls:profession ?profession. ?y ls:profession ?profession. } UNION { ?x ls:religion ?religion. ?y ls:religion ?religion. } UNION { ?x ls:ethnicity ?ethnicity. ?y ls:ethnicity ?ethnicity. } UNION { ?x ls:height ?height. ?y ls:height ?height. } UNION { ?x ls:weight ?weight. ?y ls:weight ?weight. } UNION { ?x ls:language ?language. ?y ls:language ?language. } UNION { ?x ls:parent ?parent. ?y ls:name ?parent. } UNION { ?x ls:child ?child. ?y ls:name ?child. } UNION { ?x ls:sibling ?sibling. ?y ls:name ?sibling. } UNION { ?x ls:spouse ?spouse. ?y ls:name ?spouse. } UNION { ?x <http://linkedstars.org/institute/id> ?instituteX. ?y <http://linkedstars.org/institute/id> ?instituteY. ?instituteX <http://linkedstars.org/institute/name> ?institute. ?instituteY <http://linkedstars.org/institute/name> ?institute. OPTIONAL { ?instituteX <http://linkedstars.org/institute/degree> ?degreeX}. OPTIONAL { ?instituteX <http://linkedstars.org/institute/degree> ?majorX}. OPTIONAL { ?instituteX <http://linkedstars.org/institute/startdate> ?inststartX}. OPTIONAL { ?instituteX <http://linkedstars.org/institute/enddate> ?instendX}. OPTIONAL { ?instituteX <http://linkedstars.org/institute/specialization> ?specialX}. OPTIONAL { ?instituteY <http://linkedstars.org/institute/degree> ?degreeY}. OPTIONAL { ?instituteY <http://linkedstars.org/institute/degree> ?majorY}. OPTIONAL { ?instituteY <http://linkedstars.org/institute/startdate> ?inststartY}. OPTIONAL { ?instituteY <http://linkedstars.org/institute/enddate> ?instendY}. OPTIONAL { ?instituteY <http://linkedstars.org/institute/enddate> ?specialY}. } UNION { ?x <http://linkedstars.org/employer/id> ?employerX. ?y <http://linkedstars.org/employer/id> ?employerY. ?employerX <http://linkedstars.org/employer/name> ?employer. ?employerY <http://linkedstars.org/employer/name> ?employer. OPTIONAL { ?employerX <http://linkedstars.org/employer/emptitle> ?emptitleX}. OPTIONAL { ?employerX <http://linkedstars.org/employer/startdate> ?empstartX}. OPTIONAL { ?employerX <http://linkedstars.org/employer/enddate> ?empendX}. OPTIONAL { ?employerY <http://linkedstars.org/employer/emptitle> ?emptitleY}. OPTIONAL { ?employerY <http://linkedstars.org/employer/startdate> ?empstartY}. OPTIONAL { ?employerY <http://linkedstars.org/employer/enddate> ?empendY}. } UNION { ?x <http://linkedstars.org/location/id> ?locationX. ?y <http://linkedstars.org/location/id> ?locationY. ?locationX <http://linkedstars.org/location/name> ?location. ?locationY <http://linkedstars.org/location/name> ?location. OPTIONAL {?locationX <http://linkedstars.org/location/startdate> ?locstartX}. OPTIONAL {?locationX <http://linkedstars.org/location/enddate> ?locendX}. OPTIONAL {?locationY <http://linkedstars.org/location/startdate> ?locstartY}. OPTIONAL {?locationY <http://linkedstars.org/location/enddate> ?locendY}. } }";		
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
            	
           JSONObject peopleAttributes = new JSONObject();
           JSONArray peopleAttributesArray = new JSONArray();
           
           JSONArray birthPlaceArray = new JSONArray();
           JSONArray nationalityArray = new JSONArray();
           JSONArray professionArray = new JSONArray();
           JSONArray religionArray = new JSONArray();
           JSONArray ethnicityArray = new JSONArray();
           JSONArray heightArray = new JSONArray();
           JSONArray weightArray = new JSONArray();
           JSONArray languageArray = new JSONArray();
           JSONArray familyArray = new JSONArray();  
           JSONArray instituteArray = new JSONArray();
           JSONArray employerArray = new JSONArray();
           JSONArray locationArray = new JSONArray();
           
           
           while(result.hasNext())
           {
                BindingSet bindingSet = result.next();
                
                String birthplace = stripQuotes(bindingSet.getValue("birthplace"));
                String nationality= stripQuotes(bindingSet.getValue("nationality"));
                String profession = stripQuotes(bindingSet.getValue("profession"));
                String religion = stripQuotes(bindingSet.getValue("religion"));
                String ethnicity = stripQuotes(bindingSet.getValue("ethnicity"));
                String height = stripQuotes(bindingSet.getValue("height"));
                String weight = stripQuotes(bindingSet.getValue("weight"));
                String language = stripQuotes(bindingSet.getValue("language"));
                
                String parent = stripQuotes(bindingSet.getValue("parent"));
                String child  = stripQuotes(bindingSet.getValue("child"));
                String sibling= stripQuotes(bindingSet.getValue("sibling"));
                String spouse = stripQuotes(bindingSet.getValue("spouse"));
                
                String institute = stripQuotes(bindingSet.getValue("institute"));
                String degreeX = stripQuotes(bindingSet.getValue("degreeX"));
                String majorX = stripQuotes(bindingSet.getValue("majorX"));
                String inststartX = stripQuotes(bindingSet.getValue("inststartX"));
                String instendX = stripQuotes(bindingSet.getValue("instendX"));
                String specialX = stripQuotes(bindingSet.getValue("specialX"));
                String degreeY = stripQuotes(bindingSet.getValue("degreeY"));
                String majorY = stripQuotes(bindingSet.getValue("majorY"));
                String inststartY = stripQuotes(bindingSet.getValue("inststartY"));
                String instendY = stripQuotes(bindingSet.getValue("instendY"));
                String specialY = stripQuotes(bindingSet.getValue("specialY"));
                
                String employer = stripQuotes(bindingSet.getValue("employer"));
                String emptitleX = stripQuotes(bindingSet.getValue("emptitleX"));
                String empstartX = stripQuotes(bindingSet.getValue("empstartX"));
                String empendX = stripQuotes(bindingSet.getValue("empendX"));
                String emptitleY = stripQuotes(bindingSet.getValue("emptitleY"));
                String empstartY = stripQuotes(bindingSet.getValue("empstartY"));
                String empendY = stripQuotes(bindingSet.getValue("empendY"));
                
                String location = stripQuotes(bindingSet.getValue("location"));
                String locstartX = stripQuotes(bindingSet.getValue("locstartX"));
                String locendX = stripQuotes(bindingSet.getValue("locendX"));
                String locstartY = stripQuotes(bindingSet.getValue("locstartY"));
                String locendY = stripQuotes(bindingSet.getValue("locendY"));
                
                if(birthplace!=null)
                    birthPlaceArray.put(new JSONObject().put("name", birthplace));
                if(nationality!=null)
                	nationalityArray.put(new JSONObject().put("name", nationality));	
                if(profession!=null)	
                	professionArray.put(new JSONObject().put("name", profession));
                if(religion!=null)	
                	religionArray.put(new JSONObject().put("name", religion));
                if(ethnicity!=null)	
                	ethnicityArray.put(new JSONObject().put("name", ethnicity));
                if(height!=null)	
                	heightArray.put(new JSONObject().put("name", height));
                if(weight!=null)	
                	weightArray.put(new JSONObject().put("name", weight));
                if(language!=null)	
                	languageArray.put(new JSONObject().put("name", language));
                
                if(parent!=null)
                {
                	familyArray.put(new JSONObject().put("name", "Parent"));
                }
                else if(child!=null)
                {
                	familyArray.put(new JSONObject().put("name", "Child"));
                }
                else if(sibling!=null)
                {
                	familyArray.put(new JSONObject().put("name", "Sibling"));
                }
                else if(spouse!=null)
                {
                	familyArray.put(new JSONObject().put("name", "Spouse"));
                }
                	
                if(institute!=null)
                {
                	JSONArray instituteDetails =  new JSONArray();
                	
                	if(degreeX!=null)
                		instituteDetails.put(new JSONObject().put("name", "degree1:"+degreeX));
                	if(majorX!=null)
                		instituteDetails.put(new JSONObject().put("name", "major1:"+majorX));
                	if(inststartX!=null)
                		instituteDetails.put(new JSONObject().put("name", "start-date1:"+inststartX));
                	if(instendX!=null)
                		instituteDetails.put(new JSONObject().put("name", "end-date1:"+instendX));
                	if(specialX!=null)
                		instituteDetails.put(new JSONObject().put("name", "specialization1:"+specialX));
                	
                	if(degreeY!=null)
                		instituteDetails.put(new JSONObject().put("name", "degree2:"+degreeY));
                	if(majorY!=null)
                		instituteDetails.put(new JSONObject().put("name", "major2:"+majorY));
                	if(inststartY!=null)
                		instituteDetails.put(new JSONObject().put("name", "start-date2:"+inststartY));
                	if(instendY!=null)
                		instituteDetails.put(new JSONObject().put("name", "end-date2:"+instendY));
                	if(specialY!=null)
                		instituteDetails.put(new JSONObject().put("name", "specialization2:"+specialY));
                	
                	if(instituteDetails.length()>0)
                		instituteArray.put(new JSONObject().put("name", institute).put("children", instituteDetails));
                	else
                		instituteArray.put(new JSONObject().put("name", institute));
                }
                
                if(employer!=null)
                {
                	JSONArray employerDetails =  new JSONArray();
                	
                	if(emptitleX!=null)
                		employerDetails.put(new JSONObject().put("name", "emp-title1:"+emptitleX));
                	if(empstartX!=null)
                		employerDetails.put(new JSONObject().put("name", "start-date1:"+empstartX));
                	if(empendX!=null)
                		employerDetails.put(new JSONObject().put("name", "end-date1:"+empendX));

                	if(emptitleY!=null)
                		employerDetails.put(new JSONObject().put("name", "emp-title2:"+emptitleY));
                	if(empstartY!=null)
                		employerDetails.put(new JSONObject().put("name", "start-date2:"+empstartY));
                	if(empendY!=null)
                		employerDetails.put(new JSONObject().put("name", "end-date2:"+empendY));
                	
                	if(employerDetails.length()>0)
                		employerArray.put(new JSONObject().put("name", employer).put("children", employerDetails));
                	else
                		employerArray.put(new JSONObject().put("name", employer));
                }
                
                if(location!=null)
                {
                	JSONArray locationDetails =  new JSONArray();
 
                	if(locstartX!=null)
                		locationDetails.put(new JSONObject().put("name", "start-date1:"+locstartX));
                	if(locendX!=null)
                		locationDetails.put(new JSONObject().put("name", "end-date1:"+locendX));

                	if(locstartY!=null)
                		locationDetails.put(new JSONObject().put("name", "start-date2:"+locstartY));
                	if(locendY!=null)
                		locationDetails.put(new JSONObject().put("name", "end-date2:"+locendY));
                	
                	if(locationDetails.length()>0)
                		locationArray.put(new JSONObject().put("name", location).put("children", locationDetails));
                	else
                		locationArray.put(new JSONObject().put("name", location));
                }
           }

           if(birthPlaceArray.length()>0)
        	   peopleAttributesArray.put(new JSONObject().put("name", "Birth Place").put("children", birthPlaceArray));
           
           if(nationalityArray.length()>0)
        	   peopleAttributesArray.put(new JSONObject().put("name", "Nationality").put("children", nationalityArray));
           
           if(professionArray.length()>0)
        	   peopleAttributesArray.put(new JSONObject().put("name", "Profession").put("children", professionArray));
           
           if(religionArray.length()>0)
        	   peopleAttributesArray.put(new JSONObject().put("name", "Religion").put("children", religionArray));

           if(ethnicityArray.length()>0)
        	   peopleAttributesArray.put(new JSONObject().put("name", "Ethnicity").put("children", ethnicityArray));
           
           if(heightArray.length()>0)
        	   peopleAttributesArray.put(new JSONObject().put("name", "Height").put("children", heightArray));

           if(weightArray.length()>0)
        	   peopleAttributesArray.put(new JSONObject().put("name", "Weight").put("children", weightArray));

           if(languageArray.length()>0)
        	   peopleAttributesArray.put(new JSONObject().put("name", "Language").put("children", languageArray));
           
           if(familyArray.length()>0)
        	   peopleAttributesArray.put(new JSONObject().put("name", "Family Relation").put("children", familyArray));

           if(instituteArray.length()>0)
        	   peopleAttributesArray.put(new JSONObject().put("name", "Institute").put("children", instituteArray));
           
           if(employerArray.length()>0)
        	   peopleAttributesArray.put(new JSONObject().put("name", "Employer").put("children", employerArray));
        	   
           if(locationArray.length()>0)
        	   peopleAttributesArray.put(new JSONObject().put("name", "Location Been").put("children", locationArray));
           

           peopleAttributes.put("name", "PeopleAttributes");
           peopleAttributes.put("children", peopleAttributesArray);
           
           result.close();
           con.close();
           
           return peopleAttributes;
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
