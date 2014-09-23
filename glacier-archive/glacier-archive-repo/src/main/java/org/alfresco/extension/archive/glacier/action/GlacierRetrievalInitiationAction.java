package org.alfresco.extension.archive.glacier.action;

import java.util.List;

import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;

/** 
 * Action for initiating a retrieval of content from AWS Glacier
 */
public class GlacierRetrievalInitiationAction extends AbstractGlacierAction {

    @Override
    protected void executeImpl(Action action, NodeRef actionedUponNodeRef)
    {
        archiveService.initiateRetrieval(actionedUponNodeRef);
    }

    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList)
    {
        // no action parameters at this time
    }

}
