package org.alfresco.extension.archive.glacier.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.extension.archive.glacier.GlacierArchiveModel;
import org.alfresco.extension.archive.glacier.util.GlacierHashGenerator;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.WebScriptException;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.glacier.AmazonGlacierAsyncClient;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.model.CreateVaultRequest;
import com.amazonaws.services.glacier.model.CreateVaultResult;
import com.amazonaws.services.glacier.model.DescribeJobRequest;
import com.amazonaws.services.glacier.model.DescribeJobResult;
import com.amazonaws.services.glacier.model.GetJobOutputRequest;
import com.amazonaws.services.glacier.model.GetJobOutputResult;
import com.amazonaws.services.glacier.model.InitiateJobRequest;
import com.amazonaws.services.glacier.model.InitiateJobResult;
import com.amazonaws.services.glacier.model.JobParameters;
import com.amazonaws.services.glacier.model.UploadArchiveRequest;
import com.amazonaws.services.glacier.model.UploadArchiveResult;
import com.amazonaws.services.s3.internal.RepeatableFileInputStream;

/**
 * AWS Glacier implementation of the archive service.
 */
public class ArchiveServiceGlacierImpl implements ArchiveService
{
    private static Log logger = LogFactory.getLog(ArchiveServiceGlacierImpl.class);
    
    private static final String TEMP_FILE_PREFIX = "glacier";
    private static final String TEMP_FILE_SUFFIX = ".tmp";
    private static final String JOB_TYPE_ARCHIVE_RETRIEVAL = "archive-retrieval";
    
    private AuthenticationService authenticationService;
    private NodeService nodeService;
    private ContentService contentService;
    
    private String accessKey;
    private String secretKey;
    private String endpoint;

    private AmazonGlacierAsyncClient glacierClient;

    /**
     * @param authenticationService the authentication service
     */
    public void setAuthenticationService(AuthenticationService authenticationService)
    {
        this.authenticationService = authenticationService;
    }
    
    /**
     * @param nodeService the node service
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    /**
     * @param contentService the content service
     */
    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
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
            glacierClient = new AmazonGlacierAsyncClient(credentials);
            glacierClient.setEndpoint(endpoint);
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
    
    private CreateVaultResult createVault(AmazonGlacierClient client, String vaultName)
    {
        // create the vault.  Idempotent request, does nothing if the vault already 
        // exists
        CreateVaultRequest request = new CreateVaultRequest()
            .withVaultName(vaultName);
        
        CreateVaultResult result = client.createVault(request);
        return result;
    }
    
    @Override
    public void archiveNode(
            NodeRef nodeRef, 
            String archiveContainerIdentifier, 
            boolean clearLocalContentStream)
    {
        // TODO What if they've archived, retrieved, and want to archive again?
        if (nodeService.hasAspect(nodeRef, GlacierArchiveModel.ASPECT_ARCHIVED))
        {
            throw new IllegalStateException(
                    "Node " + nodeRef.toString() + " already archived in Glacier");
        }
        
        createVault(getGlacierClient(), archiveContainerIdentifier);
        
        // prep the node for archiving (add aspect, set status, etc)
        prepArchive(nodeRef);
        
        // if the vault creation was successful (or it already existed), proceed
        // with the upload
        uploadArchive(getGlacierClient(), nodeRef, archiveContainerIdentifier, clearLocalContentStream);
    }
    
    @Override
    public void initiateRetrieval(NodeRef nodeRef)
    {
        String archiveId = (String) nodeService.getProperty(
                nodeRef, GlacierArchiveModel.PROP_ARCHIVEID);
        String locationUri = (String) nodeService.getProperty(
                nodeRef, GlacierArchiveModel.PROP_LOCATIONURI);
        String existingRetrievalJobId = (String) nodeService.getProperty(
                nodeRef, GlacierArchiveModel.PROP_RETRIEVAL_JOB_ID);
        
        if (archiveId == null || locationUri == null)
        {
            throw new IllegalStateException(
                    "Glacier retrieval can only be initiated for archived nodes");
        }
        if (existingRetrievalJobId != null && !existingRetrievalJobId.equals(""))
        {
            throw new IllegalStateException(
                    "Glacier retrieval already initiated for node " + nodeRef.toString());
        }
        
        String vaultName = getVaultNameFromLocationUri(locationUri);
        String jobDescription = "Retrieval for node " + nodeRef.toString();
        
        JobParameters jobParameters = new JobParameters()
            .withArchiveId(archiveId)
            .withDescription(jobDescription)
            // .withRetrievalByteRange("*** provide a retrieval range***") // TODO chunked download
            .withType(JOB_TYPE_ARCHIVE_RETRIEVAL);
        
        InitiateJobResult initiateJobResult = getGlacierClient().initiateJob(new InitiateJobRequest()
            .withJobParameters(jobParameters)
            .withVaultName(vaultName));
        
        String retrievalJobId = initiateJobResult.getJobId();
        
        Map<QName, Serializable> retrievalProperties = new HashMap<QName, Serializable>();
        retrievalProperties.put(GlacierArchiveModel.PROP_RETRIEVAL_INITIATED_BY, 
                authenticationService.getCurrentUserName());
        retrievalProperties.put(GlacierArchiveModel.PROP_RETRIEVAL_JOB_ID, 
                retrievalJobId);
        retrievalProperties.put(GlacierArchiveModel.PROP_RETRIEVAL_STATUS, 
                GlacierArchiveModel.RetrievalStatus.INPROGRESS);
        nodeService.addProperties(nodeRef, retrievalProperties);
        
        // add marker aspect for retrieval
        nodeService.addAspect(nodeRef, GlacierArchiveModel.ASPECT_RETRIEVAL_INITIATED, null);
    }
    
    @Override
    public boolean isRetrievalComplete(NodeRef nodeRef)
    {
        String retrievalJobId = (String) nodeService.getProperty(
                nodeRef, GlacierArchiveModel.PROP_RETRIEVAL_JOB_ID);
        String locationUri = (String) nodeService.getProperty(
                nodeRef, GlacierArchiveModel.PROP_LOCATIONURI);
        if (retrievalJobId == null || locationUri == null)
        {
            throw new IllegalStateException(
                    "Check of Glacier retrieval status can only be checked after the job has been initiated");
        }
        String vaultName = getVaultNameFromLocationUri(locationUri);
        
        DescribeJobRequest describeJobRequest = new DescribeJobRequest(vaultName, retrievalJobId);
        DescribeJobResult describeJobResult = getGlacierClient().describeJob(describeJobRequest);
        
        boolean isComplete = describeJobResult.isCompleted();
        
        if (isComplete)
        {
            Map<QName, Serializable> retrievalProperties = new HashMap<QName, Serializable>();
            retrievalProperties.put(GlacierArchiveModel.PROP_RETRIEVAL_STATUS, 
                    GlacierArchiveModel.RetrievalStatus.RETRIEVED);
            nodeService.setProperties(nodeRef, retrievalProperties);
        }
        return isComplete;
    }
    
    @Override
    public void restoreRetrieval(NodeRef nodeRef)
    {
        if (!isRetrievalComplete(nodeRef))
        {
            throw new IllegalStateException(
                    "Restore of Glacier retrieval can only be attempted for completed retrieval requests");
        }
        String retrievalJobId = (String) nodeService.getProperty(
                nodeRef, GlacierArchiveModel.PROP_RETRIEVAL_JOB_ID);
        String locationUri = (String) nodeService.getProperty(
                nodeRef, GlacierArchiveModel.PROP_LOCATIONURI);
        String vaultName = getVaultNameFromLocationUri(locationUri);
        
        GetJobOutputRequest jobOutputRequest = new GetJobOutputRequest()
            .withJobId(retrievalJobId)
            .withVaultName(vaultName);
        GetJobOutputResult jobOutputResult = getGlacierClient().getJobOutput(jobOutputRequest);
        
        ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
        writer.putContent(jobOutputResult.getBody());
        
        Map<QName, Serializable> retrievalProperties = new HashMap<QName, Serializable>();
        retrievalProperties.put(GlacierArchiveModel.PROP_RETRIEVAL_STATUS, 
                GlacierArchiveModel.RetrievalStatus.RESTORED);
        nodeService.setProperties(nodeRef, retrievalProperties);
    }
    
    private void prepArchive(NodeRef node)
    {
        Map<QName, Serializable> archiveProperties = new HashMap<QName, Serializable>();
        archiveProperties.put(GlacierArchiveModel.PROP_ARCHIVE_INITIATED_BY, 
                authenticationService.getCurrentUserName());
        archiveProperties.put(GlacierArchiveModel.PROP_ARCHIVE_STATUS,
                GlacierArchiveModel.ArchiveStatus.INPROGRESS);
        nodeService.addAspect(node, GlacierArchiveModel.ASPECT_ARCHIVED, archiveProperties);
    }
    
    private void uploadArchive(
            AmazonGlacierAsyncClient client,
            NodeRef toArchive,
            String vaultName,
            boolean clearLocalContentStream)
    {
        try {
            
            UploadArchiveRequest request = new UploadArchiveRequest()
                .withArchiveDescription(String.valueOf(nodeService.getProperty(toArchive, ContentModel.PROP_NAME)))
                .withChecksum(generateChecksum(toArchive))
                .withVaultName(vaultName)
                .withBody(new RepeatableFileInputStream(stream2file(getContentInputStream(toArchive))))
                .withContentLength(getContentSize(toArchive));
            
            // NTM - this needs to be async, once I sort out some kind of callback
            // mechanism and notification framework for Share
            client.uploadArchiveAsync(request, 
                    new GlacierArchiveResponseHandler(
                            toArchive, 
                            authenticationService.getCurrentUserName(), 
                            clearLocalContentStream));
            
        } catch (Exception e) {
            throw new WebScriptException("Unable to send doc to AWS Glacier", e);
        }
    }
    
    /**
     * Gets the vault name from the archive location URI.
     * 
     * @param locationUri
     * @return the vault name
     */
    protected String getVaultNameFromLocationUri(String locationUri)
    {
        Pattern pattern = Pattern.compile("/vaults/(.*?)/archives/");
        Matcher matcher = pattern.matcher(locationUri);
        if (!matcher.find())
        {
            throw new IllegalStateException("Could not determine vault name from locationUri");
        }
        return matcher.group(1);
    }
    
    public static File stream2file (InputStream in) throws IOException {
        final File tempFile = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.copy(in, out);
        }
        return tempFile;
    }
    
    private String generateChecksum(NodeRef node)
    {
        ContentReader reader = contentService.getReader(node, ContentModel.PROP_CONTENT);
        return GlacierHashGenerator.getHash(reader);
    }
    
    private long getContentSize(NodeRef node) {
        ContentReader reader = contentService.getReader(node, ContentModel.PROP_CONTENT);
        return reader.getSize();
    }
    
    private InputStream getContentInputStream(NodeRef node)
    {
        ContentReader reader = contentService.getReader(node, ContentModel.PROP_CONTENT);
        return reader.getContentInputStream();
    }
    
    private class GlacierArchiveResponseHandler implements AsyncHandler<UploadArchiveRequest, UploadArchiveResult>
    {
        private NodeRef archivedNode;
        private String user;
        private boolean clearLocalContentStream;
        
        public GlacierArchiveResponseHandler(NodeRef archivedNode, String user, boolean clearLocalContentStream)
        {
            this.archivedNode = archivedNode;
            this.user = user;
            this.clearLocalContentStream = clearLocalContentStream;
        }
        
        @Override
        public void onError(Exception ex) 
        {
            logger.error(ex.getMessage(), ex);
            
            // does the node still exist?  Was it deleted before the archive
            // operation could complete?
            Boolean rtn = AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Boolean>() {
                @Override
                public Boolean doWork() throws Exception {
                    
                    if(nodeService.exists(archivedNode))
                    {
                        Map<QName, Serializable> archiveProperties = new HashMap<QName, Serializable>();
                        archiveProperties.put(GlacierArchiveModel.PROP_ARCHIVE_STATUS, GlacierArchiveModel.ArchiveStatus.FAILED);
                        nodeService.setProperties(archivedNode, archiveProperties);
                    }
                    else
                    {
                        logger.error("NodeRef " + archivedNode + " does not exist in repository, but was archived to AWS Glacier");
                    }
                    
                    return true;
                }
            }, user);
        }

        @Override
        public void onSuccess(UploadArchiveRequest request, final UploadArchiveResult response) 
        {
            // if the request was successful, persist the info required to 
            // retrieve the content.  Delete the content stream if configured to
            // do so.
            
            // does the node still exist?  Was it deleted before the archive
            // operation could complete?
            Boolean rtn = AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Boolean>() {
                @Override
                public Boolean doWork() throws Exception {
                    
                    if(nodeService.exists(archivedNode))
                    {
                        Map<QName, Serializable> archiveProperties = new HashMap<QName, Serializable>();
                        archiveProperties.put(GlacierArchiveModel.PROP_ARCHIVEID, response.getArchiveId());
                        archiveProperties.put(GlacierArchiveModel.PROP_GLACIERCHECKSUM, response.getChecksum());
                        archiveProperties.put(GlacierArchiveModel.PROP_LOCATIONURI, response.getLocation());
                        archiveProperties.put(GlacierArchiveModel.PROP_ARCHIVE_STATUS, GlacierArchiveModel.ArchiveStatus.ARCHIVED);
                        nodeService.addProperties(archivedNode, archiveProperties);
                        
                        // if we are configured to clear the content stream, do so now
                        if(clearLocalContentStream)
                        {
                            // TODO Investigate setting to empty content stream rather than text
                            ContentWriter writer = contentService.getWriter(
                                    archivedNode, ContentModel.PROP_CONTENT, true);
                            writer.putContent("Archived to AWS Glacier");
                        }
                    }
                    else
                    {
                        logger.error("NodeRef " + archivedNode + " does not exist in repository, but was archived to AWS Glacier");
                    }
                    
                    return true;
                }
            }, user);
        }
    }

}
