package org.alfresco.extension.archive.glacier.service;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Defines a service responsible for archiving and retrieving content
 * by means which aren't suited for synchronous calls.
 */
public interface ArchiveService
{
    
    /**
     * Archives the given node ref into the given archive container, optionally 
     * clearing out the local content stream object upon successful archive.
     * 
     * @param nodeRef the node ref to be archived
     * @param archiveContainerIdentifier the container for the archive, i.e. Glacier vault
     * @param clearLocalContentStream whether or not to clear out the local repo content stream
     */
    public void archiveNode(
            NodeRef nodeRef, 
            String archiveContainerIdentifier, 
            boolean clearLocalContentStream);
    
    /**
     * Initiate the retrieval of the given node ref from the archive container.
     * <p>
     * Implementations must persist all data needed to retrieve the content as properties
     * on the node.
     * 
     * @param nodeRef the node ref to be retrieved
     */
    public void initiateRetrieval(NodeRef nodeRef);

}
