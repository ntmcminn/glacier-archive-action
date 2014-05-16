package org.alfresco.extension.archive.glacier.action;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.glacier.AmazonGlacierAsyncClient;

/**
 * Base abstract AWS Glacier action
 */
public abstract class AbstractGlacierAction extends ActionExecuterAbstractBase
{

    protected AuthenticationService authenticationService;
    protected NodeService nodeService;
    
    private String accessKey;
    private String secretKey;
    private String endpoint;

    private AmazonGlacierAsyncClient glacierClient;

    /**
     * @param nodeService the node service
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    /**
     * @param authenticationService the authentication service
     */
    public void setAuthenticationService(AuthenticationService authenticationService)
    {
        this.authenticationService = authenticationService;
    }

    /**
     * Gets or creates an async AWS Glacier client
     * 
     * @return the Glacier client
     */
    protected AmazonGlacierAsyncClient getGlacierClient()
    {
        if (glacierClient == null)
        {
            BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            AmazonGlacierAsyncClient client = new AmazonGlacierAsyncClient(credentials);
            client.setEndpoint(endpoint);
        }
        return glacierClient;
    }

    /**
     * Sets the AWS access key
     * 
     * @param accessKey
     */
    public void setAccessKey(String accessKey)
    {
        this.accessKey = accessKey;
    }

    /**
     * Sets the AWS secret
     * 
     * @param secretKey
     */
    public void setSecretKey(String secretKey)
    {
        this.secretKey = secretKey;
    }

    /**
     * Set the AWS endpoint
     * 
     * @param endpoint
     */
    public void setEndpoint(String endpoint)
    {
        this.endpoint = endpoint;
    }

}
