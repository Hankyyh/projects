package openrdf;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.List;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.repository.manager.RemoteRepositoryManager;
import org.openrdf.repository.sail.config.SailRepositoryConfig;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.config.SailImplConfig;
import org.openrdf.sail.memory.config.MemoryStoreConfig;

public class Openrdf {

		
		public static boolean create_repo(String repo_name ) {
			
			// create a configuration for the SAIL stack
			SailImplConfig backendConfig = new MemoryStoreConfig();
			
			// create a configuration for the repository implementation
			RepositoryImplConfig repositoryTypeSpec = new SailRepositoryConfig(backendConfig);
			
			String serverUrl = "http://localhost:8080/openrdf-sesame";
			RemoteRepositoryManager manager = new RemoteRepositoryManager(serverUrl);
			try {
				manager.initialize();
			} catch (RepositoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			RepositoryConfig repConfig = new RepositoryConfig(repo_name, repositoryTypeSpec);
			try {
				manager.addRepositoryConfig(repConfig);
			} catch (RepositoryException | RepositoryConfigException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 
			try {
				Repository repository = manager.getRepository(repo_name);
				System.out.println(repository);
			} catch (RepositoryConfigException | RepositoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			System.out.println(repository);
			return true;
			
		}
		
		
		
		public static void main(String[] args) throws RepositoryException, RDFParseException, IOException, MalformedQueryException, QueryEvaluationException {
			
			if(create_repo("csci549")){
				System.out.println("Repository created");
//				System.exit(0);
			}else{
				System.out.println("Repository creation failed");
				System.exit(0);
			}
			
			String sesameserver = "http://localhost:8080/openrdf-sesame";
			String repositoryID = "csci549";
			Repository myRepo = new HTTPRepository(sesameserver,repositoryID);
			try {
				myRepo.initialize();
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
			
	// Adding rdf data to the repository
			File file = new File( "/Users/vineet/Desktop/CSCI548/HomeWorks/3&4/csci-548.ttl");
			File file2 = new File( "/Users/vineet/Desktop/CSCI548/HomeWorks/3&4/friends.ttl");
			String baseURI = "file://";
			RepositoryConnection con=null;
				con = myRepo.getConnection();
				con.add(file, baseURI,RDFFormat.TURTLE);
				con.add(file2, baseURI,RDFFormat.TURTLE);
				URL url = new URL("http://localhost:8080/openrdf-workbench/repositories/csci549/");
				con.add(url, url.toString(), RDFFormat.TURTLE);
		
				
	//Question 3-Query 1: Counting the number of MS student
				
			String query1 = "select (count(?student) as ?sCount) where {?student a <http://example.com/students/Student> ; <http://example.com/students/objective> 'MS' }";
				TupleQuery tupleQuery1 = con.prepareTupleQuery(QueryLanguage.SPARQL, query1);
			      TupleQueryResult result1 = tupleQuery1.evaluate();
			      List<String> bindingNames1 = result1.getBindingNames();
			      BindingSet bindingSet1 = result1.next();
		   	   	Value stud_count = bindingSet1.getValue(bindingNames1.get(0));
		   	   	System.out.println("MS students name: "+stud_count);


	//Question 4-Query 2 Counting the no of friends
		   	   	
		   	String query2 = "SELECT ?name (COUNT(?name) AS ?NoOfFriends)"+ 
		   			" WHERE {"+ 
		   			" ?s1 <http://xmlns.com/foaf/0.1/mbox> ?s1_email ;"+
		   			" <http://xmlns.com/foaf/0.1/knows> ?s2 ."+ 
		   			" ?s2 <http://xmlns.com/foaf/0.1/mbox> ?s2_email ."+   
	 
					" ?student <http://xmlns.com/foaf/0.1/name> ?name ."+ 
					" ?student <http://xmlns.com/foaf/0.1/mbox> ?s1_email . }"+ 
					" GROUP BY ?name ORDER BY ?name";
		   	
		   	FileWriter writer = null;
		   	writer = new FileWriter("question4.csv");
		   	PrintWriter pw = new PrintWriter(writer);
		   	
		   	TupleQuery tupleQuery2 = con.prepareTupleQuery(QueryLanguage.SPARQL, query2);
		   	TupleQueryResult result2 = tupleQuery2.evaluate();
		   	List<String> bindingNames2 = result2.getBindingNames();
		   	while(result2.hasNext()){
		   		BindingSet bindingSet2 = result2.next();
		   		pw.append(bindingSet2.getValue(bindingNames2.get(0)).toString());
		   		pw.append(",");
		   		pw.append(bindingSet2.getValue(bindingNames2.get(1)).toString());
		   		pw.append("\n");   		
		   	}
		   	pw.flush();
		   	pw.close();
		   	writer.close();
		   
		   		
		   	
	//Question 5-Query 3 adding pedro and making him friend of everyone. Creating friend list.
			ValueFactory factory = myRepo.getValueFactory();
			URI student = factory.createURI("http://example.com/students/Student");
			Literal pedroName = factory.createLiteral("Szekely, Pedro ");
			Literal pedroEmail = factory.createLiteral("pszekely@isi.edu");

			
			RepositoryResult<Statement> statements = con.getStatements(null, FOAF.MBOX, null,false);
			try{
				int count=0;
			while (statements.hasNext()) {
				count++;
			      Statement st = statements.next();
			      System.out.println(st.getObject());
			      System.out.println(st.getSubject());
			      System.out.println(st.getPredicate());
			      
			      Resource sub = st.getSubject();
			      URI pre = st.getPredicate();
			      Value obj = st.getObject();
			     if(con.hasStatement(sub, FOAF.NAME, null, true)){
			    	  RepositoryResult<Statement> statements1 = con.getStatements(null, FOAF.MBOX, obj, true);
			    	  Statement st1 = statements1.next();
			    	  Resource sub1 = st1.getSubject();
			    	  BNode bnode1 = factory.createBNode();
			    	  con.add(bnode1, RDF.TYPE, student);
			    	  con.add(bnode1, FOAF.MBOX, pedroEmail);
			    	  con.add(sub1, FOAF.KNOWS, bnode1);
			    	  
				      count++;
				     
			      }else{
			    	  continue;
			      }     
			}
			System.out.println("Count = "+count);
			}finally{
				
				statements.close();
			}
			BNode bnode = factory.createBNode();
			
			con.add(bnode, RDF.TYPE, student);
			con.add(bnode, FOAF.NAME, pedroName);
			con.add(bnode, FOAF.MBOX, pedroEmail);
			
			String query3 = "";
				
			FileWriter writer2 = null;
		   	writer2 = new FileWriter("question5.csv");
		   	PrintWriter pw2 = new PrintWriter(writer2);
		   	
		   	TupleQuery tupleQuery3 = con.prepareTupleQuery(QueryLanguage.SPARQL, query3);
		   	TupleQueryResult result3 = tupleQuery3.evaluate();
		   	List<String> bindingNames3 = result3.getBindingNames();
		   	while(result3.hasNext()){
		   		BindingSet bindingSet3 = result3.next();
		   		pw.append(bindingSet3.getValue(bindingNames3.get(0)).toString());
		   		pw.append(",");
		   		pw.append(bindingSet3.getValue(bindingNames3.get(1)).toString());
		   		pw.append("\n");   		
		   	}
		   	pw2.flush();
		   	pw2.close();
		   	writer2.close();
				
			
	//Questioin 6-Query 4
		   	
		   	
		   	
		   	
			}
	


	
}
