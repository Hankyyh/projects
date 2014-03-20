package linkedstars.dataaccess.repository;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.rio.RDFFormat;

public class PopulateRepository {

    public static void main(String[] args) 
    {
        String repositoryID = "LinkedStars";
        
        //CREATE REPOSITORY
        try 
        {   
            RepositoryManager repoManager = new RepositoryManager();
            repoManager.initializeRepoManager(repositoryID);
        } 
        catch (RepositoryException  ex) 
        {
            Logger.getLogger(RepositoryManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch(RepositoryConfigException rce)
        {
            Logger.getLogger(RepositoryManager.class.getName()).log(Level.SEVERE, null, rce);
        }
        
        //ACCESS REPOSITORY
        Repository myRepository = new HTTPRepository(RepositoryConstants.sesameServer, repositoryID);
        RepositoryConnection conn = null;

        try 
        { 
            myRepository.initialize();
            conn = myRepository.getConnection();
        } 
        catch (RepositoryException ex) 
        {
            System.out.println("Error Accessing Repository..."+ex.getMessage());
            System.exit(-1);
        }
        
        //ADD FILES TO REPOSITORY
        File dir = new File("C:\\Users\\TarneetSingh\\Documents\\NetBeansProjects\\LinkedStars\\rdf_files");
        File[] listOfFiles = dir.listFiles();
        
        for(int i=0; i<listOfFiles.length; i++)
        {
            if (listOfFiles[i].isFile()) 
            {
                try 
                {
                    //UPLOAD RDF TO SERVER
                    conn.begin(); // start a transaction 
                    conn.add(listOfFiles[i], RepositoryConstants.baseURI, RDFFormat.TURTLE);
                    conn.commit(); // both files are actually added to the store now, transaction has ended.
                    conn.close();
                }
                catch (Exception ex)
                {
                    System.out.println("Error while uploading RDF to server: "+ex.getMessage());
                    System.exit(-1);
                }
                
                System.out.println("File '" + listOfFiles[i].getName()+"' uploaded to server...");
            }
        }
    }
}
