package org.alfresco.extension.archive.glacier.action;

import org.alfresco.extension.archive.glacier.service.ArchiveService;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;

/**
 * Base abstract AWS Glacier action
 */
public abstract class AbstractGlacierAction extends ActionExecuterAbstractBase
{
    protected ArchiveService archiveService;
	protected String vaultName;
	
    public void setArchiveService(ArchiveService archiveService)
    {
        this.archiveService = archiveService;
    }
    
	public void setVaultName(String vaultName) {
		this.vaultName = vaultName;
	}
}
