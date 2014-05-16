package org.alfresco.extension.archive.glacier.action;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.extension.archive.glacier.GlacierArchiveModel;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import com.amazonaws.services.glacier.model.InitiateJobRequest;
import com.amazonaws.services.glacier.model.InitiateJobResult;
import com.amazonaws.services.glacier.model.JobParameters;

/**
 * Action for initiating a retrieval of content from AWS Glacier
 */
public class GlacierRetrievalInitiationAction extends AbstractGlacierAction {
    
    private static final String JOB_TYPE_ARCHIVE_RETRIEVAL = "archive-retrieval";

    @Override
    protected void executeImpl(Action action, NodeRef actionedUponNodeRef)
    {
        String archiveId = (String) nodeService.getProperty(
                actionedUponNodeRef, GlacierArchiveModel.PROP_ARCHIVEID);
        String locationUri = (String) nodeService.getProperty(
                actionedUponNodeRef, GlacierArchiveModel.PROP_LOCATIONURI);
        if (archiveId == null || locationUri == null)
        {
            throw new IllegalStateException(
                    "Glacier retrieval can only be initiated for archived nodes");
        }
        String vaultName = getVaultNameFromLocationUri(locationUri);
        String jobDescription = "Retrieval for node " + actionedUponNodeRef.toString();
        
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
        nodeService.setProperties(actionedUponNodeRef, retrievalProperties);
    }

    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList)
    {
        // no action parameters at this time
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

}
