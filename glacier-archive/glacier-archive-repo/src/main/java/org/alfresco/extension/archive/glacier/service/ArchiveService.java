package org.alfresco.extension.archive.glacier.service;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Defines a service responsible for archiving and retrieving content
 * by means which aren't suited for synchronous calls.
 */
public interface ArchiveService
{
    
    public void archiveNode(
            NodeRef nodeRef, 
            String archiveContainerIdentifier, 
            boolean clearLocalContentStream);
    
    public void initiateRetrieval(NodeRef nodeRef);

}
