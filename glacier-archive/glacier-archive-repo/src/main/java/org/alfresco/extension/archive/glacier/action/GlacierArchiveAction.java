package org.alfresco.extension.archive.glacier.action;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.extension.archive.glacier.GlacierArchiveModel;
import org.alfresco.extension.archive.glacier.util.GlacierArchiveUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
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
import com.amazonaws.services.glacier.model.UploadArchiveRequest;
import com.amazonaws.services.glacier.model.UploadArchiveResult;
import com.amazonaws.services.s3.internal.RepeatableFileInputStream;

public class GlacierArchiveAction extends ActionExecuterAbstractBase 
{
	private static Log logger = LogFactory.getLog(GlacierArchiveAction.class);
	
	private ServiceRegistry registry;
	private GlacierArchiveUtil glacierUtil;
	private boolean deleteContentStream = false;
	private String vaultName;
	private String accessKey;
	private String secretKey;
	private String endpoint;
	
	@Override
	protected void executeImpl(Action action, NodeRef actionedNode) 
	{
		
		BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
		AmazonGlacierAsyncClient client = new AmazonGlacierAsyncClient(credentials);
		client.setEndpoint(endpoint);
		
		createVault(client, vaultName);
		
		// if the vault creation was successful (or it already existed), proceed
		// with the upload
		
		uploadArchive(client, actionedNode);

	}
	
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) 
	{
		// no action parameters at this time
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
	
	private void uploadArchive(AmazonGlacierAsyncClient client, NodeRef toArchive)
	{
		NodeService ns = registry.getNodeService();
		
		try {
			
			UploadArchiveRequest request = new UploadArchiveRequest()
				.withArchiveDescription(String.valueOf(ns.getProperty(toArchive, ContentModel.PROP_NAME)))
				.withChecksum(glacierUtil.generateChecksum(toArchive))
				.withVaultName(vaultName)
				.withBody(new RepeatableFileInputStream(stream2file(glacierUtil.getContentInputStream(toArchive))))
				.withContentLength(glacierUtil.getContentSize(toArchive));
			
			// NTM - this needs to be async, once I sort out some kind of callback
			// mechanism and notification framework for Share
			client.uploadArchiveAsync(request, new GlacierArchiveResponseHandler(toArchive));
			
		} catch (Exception e) {
			throw new WebScriptException("Unable to send doc to AWS Glacier", e);
		}
		
		
	}
	
    public static final String PREFIX = "glacier";
    public static final String SUFFIX = ".tmp";

    public static File stream2file (InputStream in) throws IOException {
        final File tempFile = File.createTempFile(PREFIX, SUFFIX);
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.copy(in, out);
        }
        return tempFile;
    }	
	
	public void setVaultName(String vaultName) {
		this.vaultName = vaultName;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public void setServiceRegistry(ServiceRegistry registry)
	{
		this.registry = registry;
	}
	
	public void setDeleteContentStream(boolean deleteContentStream) {
		this.deleteContentStream = deleteContentStream;
	}
	
	public void setGlacierUtil(GlacierArchiveUtil glacierUtil) {
		this.glacierUtil = glacierUtil;
	}
	
	private class GlacierArchiveResponseHandler implements AsyncHandler<UploadArchiveRequest, UploadArchiveResult>
	{
		private NodeRef archivedNode;
		
		public GlacierArchiveResponseHandler(NodeRef archivedNode)
		{
			this.archivedNode = archivedNode;
		}
		
		@Override
		public void onError(Exception ex) 
		{
			// probably not much we can do here except log the failure
			logger.error(ex.getMessage(), ex);
		}

		@Override
		public void onSuccess(UploadArchiveRequest request, UploadArchiveResult response) 
		{
			// if the request was successful, persist the info required to 
			// retrieve the content.  Delete the content stream if configured to
			// do so.
			NodeService ns = registry.getNodeService();
			ContentService cs = registry.getContentService();
			
			// does the node still exist?  Was it deleted before the archive
			// operation could complete?
			if(ns.exists(archivedNode))
			{
				Map<QName, Serializable> archiveProperties = new HashMap<QName, Serializable>();
				archiveProperties.put(GlacierArchiveModel.PROP_ARCHIVEID, response.getArchiveId());
				archiveProperties.put(GlacierArchiveModel.PROP_GLACIERCHECKSUM, response.getChecksum());
				archiveProperties.put(GlacierArchiveModel.PROP_LOCATIONURI, response.getLocation());
				ns.addAspect(archivedNode, GlacierArchiveModel.ASPECT_ARCHIVED, archiveProperties);
				
				// if we are configured to clear the content stream, do so now
				if(deleteContentStream)
				{
					ContentWriter writer = cs.getWriter(archivedNode, ContentModel.PROP_CONTENT, true);
					writer.putContent("Archived to AWS Glacier");
				}
			}
			else
			{
				logger.error("NodeRef " + archivedNode + " does not exist in repository, but was archived to AWS Glacier");
			}
		}
		
	}

}
