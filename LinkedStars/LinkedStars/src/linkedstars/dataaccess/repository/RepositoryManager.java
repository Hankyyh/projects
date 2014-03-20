package linkedstars.dataaccess.repository;

import java.io.IOException;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.repository.sail.config.SailRepositoryConfig;
import org.openrdf.sail.config.SailImplConfig;
import org.openrdf.sail.memory.config.MemoryStoreConfig;


public class RepositoryManager
{
    SesameRepositoryManager manager;
    
    public void initializeRepoManager(String repositoryID) throws RepositoryException, RepositoryConfigException
    {
        manager = new SesameRepositoryManager(RepositoryConstants.sesameServer);            
        manager.initialize();
        
        // create a configuration for the SAIL stack
        SailImplConfig backendConfig = new MemoryStoreConfig();
        
        // create a configuration for the repository implementation
        RepositoryImplConfig repositoryTypeSpec = new SailRepositoryConfig(backendConfig);
        
        RepositoryConfig repoConfig = new RepositoryConfig(repositoryID, repositoryID, repositoryTypeSpec);
        manager.addRepositoryConfig(repoConfig);
    }
    
    
    public RepositoryConnection getConnection(String repositoryID)
    {
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

        return conn;
    }
    
    
    
    public Repository createRepository(String repositoryID) throws RepositoryConfigException, RepositoryException
    {
        if(manager != null) 
        {
            Repository myRepository = manager.createRepository(repositoryID);
            myRepository.initialize();
            return myRepository;
        }
        return null;
     }
    
    public void removeRepository(String repositoryID) throws RepositoryException, RepositoryConfigException
    {
        if(manager != null) 
        {
            manager.removeRepository(repositoryID);
        }
    }
    
    public void cleanUpRepository(String repositoryID) throws IOException
    {
        if(manager != null) 
        {
            manager.cleanUpRepository(repositoryID);
        }
    }
}

