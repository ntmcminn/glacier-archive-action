package org.alfresco.extension.archive.glacier.action;

import java.util.List;

import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;

public class GlacierArchiveAction extends AbstractGlacierAction 
{
	private boolean deleteContentStream = false;
	private String vaultName;
	
	@Override
	protected void executeImpl(Action action, NodeRef actionedNode) 
	{
	    archiveService.archiveNode(actionedNode, vaultName, deleteContentStream);
	}
	
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) 
	{
		// no action parameters at this time
	    // TODO Perhaps vaultName and deleteContentStream should be action params
	}
	
	public void setVaultName(String vaultName) {
		this.vaultName = vaultName;
	}

	public void setDeleteContentStream(boolean deleteContentStream) {
		this.deleteContentStream = deleteContentStream;
	}

}
