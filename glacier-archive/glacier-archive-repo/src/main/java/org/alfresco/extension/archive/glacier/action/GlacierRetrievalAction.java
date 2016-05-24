package org.alfresco.extension.archive.glacier.action;

import java.util.List;

import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;

public class GlacierRetrievalAction extends AbstractGlacierAction {

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		if(archiveService.isRetrievalComplete(actionedUponNodeRef)){
			archiveService.restoreRetrieval(actionedUponNodeRef);
		}else{
			throw new RuntimeException("Cannot restore an archive before the retrieval is complete");
		}
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		// No parameters at this time
	}

}
