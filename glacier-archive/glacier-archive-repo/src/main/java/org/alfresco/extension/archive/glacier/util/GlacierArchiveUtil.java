package org.alfresco.extension.archive.glacier.util;

import java.io.InputStream;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;

public class GlacierArchiveUtil 
{

	private ServiceRegistry registry;
	
	public GlacierArchiveUtil(ServiceRegistry registry)
	{
		this.registry = registry;
	}
	
	public String generateChecksum(NodeRef node)
	{
		return "";
	}
	
	public InputStream getContentInputStream(NodeRef node)
	{
		ContentService cs = registry.getContentService();
		ContentReader reader = cs.getReader(node, ContentModel.PROP_CONTENT);
		return reader.getContentInputStream();
	}
}
