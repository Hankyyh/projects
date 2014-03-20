package linkedstars.dataaccess.repository;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;

public class TestRepository {

	public static void testQuery()
	{
		// TODO Auto-generated method stub
		try 
        {
            //ACCESS REPOSITORY
			RepositoryManager repoManager = new RepositoryManager();
			RepositoryConnection con = repoManager.getConnection("LinkedStars");
           
            //QUERY AND WRITE TO FILE
            try 
            {
               String queryString = "PREFIX ls: <http://linkedstars.org/> SELECT ?birthplace ?nationality ?profession ?religion ?ethnicity ?height ?weight ?language ?institute ?degreeX ?majorX ?inststartX ?instendX ?specialX ?degreeY ?majorY ?inststartY ?instendY ?specialY ?employer ?emptitleX ?empstartX ?empendX ?emptitleY ?empstartY ?empendY ?location ?locstartX ?locendX ?locstartY ?locendY WHERE { ?x ls:name \"James Franco\". ?y ls:name \"Kal Penn\". { ?x ls:birthplace ?birthplace. ?y ls:birthplace ?birthplace. } UNION { ?x ls:nationality ?nationality. ?y ls:nationality ?nationality. } UNION { ?x ls:profession ?profession. ?y ls:profession ?profession. } UNION { ?x ls:religion ?religion. ?y ls:religion ?religion. } UNION { ?x ls:ethnicity ?ethnicity. ?y ls:ethnicity ?ethnicity. } UNION { ?x ls:height ?height. ?y ls:height ?height. } UNION { ?x ls:weight ?weight. ?y ls:weight ?weight. } UNION { ?x ls:language ?language. ?y ls:language ?language. } UNION { ?x <http://linkedstars.org/institute/id> ?instituteX. ?y <http://linkedstars.org/institute/id> ?instituteY. ?instituteX <http://linkedstars.org/institute/name> ?institute. ?instituteY <http://linkedstars.org/institute/name> ?institute. OPTIONAL { ?instituteX <http://linkedstars.org/institute/degree> ?degreeX}. OPTIONAL { ?instituteX <http://linkedstars.org/institute/degree> ?majorX}. OPTIONAL { ?instituteX <http://linkedstars.org/institute/startdate> ?inststartX}. OPTIONAL { ?instituteX <http://linkedstars.org/institute/enddate> ?instendX}. OPTIONAL { ?instituteX <http://linkedstars.org/institute/specialization> ?specialX}. OPTIONAL { ?instituteY <http://linkedstars.org/institute/degree> ?degreeY}. OPTIONAL { ?instituteY <http://linkedstars.org/institute/degree> ?majorY}. OPTIONAL { ?instituteY <http://linkedstars.org/institute/startdate> ?inststartY}. OPTIONAL { ?instituteY <http://linkedstars.org/institute/enddate> ?instendY}. OPTIONAL { ?instituteY <http://linkedstars.org/institute/enddate> ?specialY}. } UNION { ?x <http://linkedstars.org/employer/id> ?employerX. ?y <http://linkedstars.org/employer/id> ?employerY. ?employerX <http://linkedstars.org/employer/name> ?employer. ?employerY <http://linkedstars.org/employer/name> ?employer. OPTIONAL { ?employerX <http://linkedstars.org/employer/emptitle> ?emptitleX}. OPTIONAL { ?employerX <http://linkedstars.org/employer/startdate> ?empstartX}. OPTIONAL { ?employerX <http://linkedstars.org/employer/enddate> ?empendX}. OPTIONAL { ?employerY <http://linkedstars.org/employer/emptitle> ?emptitleY}. OPTIONAL { ?employerY <http://linkedstars.org/employer/startdate> ?empstartY}. OPTIONAL { ?employerY <http://linkedstars.org/employer/enddate> ?empendY}. } UNION { ?x <http://linkedstars.org/location/id> ?locationX. ?y <http://linkedstars.org/location/id> ?locationY. ?locationX <http://linkedstars.org/location/name> ?location. ?locationY <http://linkedstars.org/location/name> ?location. OPTIONAL {?locationX <http://linkedstars.org/location/startdate> ?locstartX}. OPTIONAL {?locationX <http://linkedstars.org/location/enddate> ?locendX}. OPTIONAL {?locationY <http://linkedstars.org/location/startdate> ?locstartY}. OPTIONAL {?locationY <http://linkedstars.org/location/enddate> ?locendY}. } }";
               TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

               TupleQueryResult result = tupleQuery.evaluate();
               List<String> keyNames = result.getBindingNames();
               try 
               {
                   System.out.println("\n\n\n");
                   
                   for(String key: keyNames)
                   {
                	   System.out.print(key+",");
                   }
            	   System.out.println();

                   while(result.hasNext())
                   {
                        BindingSet bindingSet = result.next();
                        
                        for(String key: keyNames)
                        {
                        	Value value = bindingSet.getValue(key);
                        	System.out.print(value+",");
                        }
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
	}
	
	
	public static void main(String[] args) 
	{
		// TODO Auto-generated method stub
		linkedstars.model.celebrity.PeopleAttributes peopleAtt = new linkedstars.model.celebrity.PeopleAttributes();
		System.out.println(peopleAtt.getAllPeopleAttributes("Johnny Galecki", "Jim Parsons", 1));
		//TestRepository.testQuery();
	}

}
