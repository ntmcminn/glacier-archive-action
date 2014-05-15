package org.alfresco.extension.archive.glacier;

import org.alfresco.service.namespace.QName;

public interface GlacierArchiveModel {

	// namespace
    static final String GLACIER_ARCHIVE_MODEL_1_0_URI = "http://www.alfresco.org/model/glacier/1.0";
    
    // archived aspect and properties
 	static final QName ASPECT_ARCHIVED = QName.createQName(GLACIER_ARCHIVE_MODEL_1_0_URI, "archived");
 	static final QName PROP_ARCHIVEID = QName.createQName(GLACIER_ARCHIVE_MODEL_1_0_URI, "archiveId");
 	static final QName PROP_GLACIERCHECKSUM = QName.createQName(GLACIER_ARCHIVE_MODEL_1_0_URI, "glacierChecksum");
 	static final QName PROP_LOCATIONURI = QName.createQName(GLACIER_ARCHIVE_MODEL_1_0_URI, "locationUri");

}
