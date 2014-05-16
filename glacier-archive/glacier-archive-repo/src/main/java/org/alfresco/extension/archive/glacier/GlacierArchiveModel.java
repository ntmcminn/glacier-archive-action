package org.alfresco.extension.archive.glacier;

import org.alfresco.service.namespace.QName;

/**
 * Data model definitions for AWS Glacier archive operations
 */
public interface GlacierArchiveModel {

	// namespace
    static final String GLACIER_ARCHIVE_MODEL_1_0_URI = "http://www.alfresco.org/model/glacier/1.0";
    
    // archived aspect and properties
 	static final QName ASPECT_ARCHIVED = QName.createQName(GLACIER_ARCHIVE_MODEL_1_0_URI, "archived");
 	static final QName PROP_ARCHIVEID = QName.createQName(GLACIER_ARCHIVE_MODEL_1_0_URI, "archiveId");
 	static final QName PROP_GLACIERCHECKSUM = QName.createQName(GLACIER_ARCHIVE_MODEL_1_0_URI, "glacierChecksum");
 	static final QName PROP_LOCATIONURI = QName.createQName(GLACIER_ARCHIVE_MODEL_1_0_URI, "locationUri");
 	static final QName PROP_ARCHIVE_INITIATED_BY = QName.createQName(GLACIER_ARCHIVE_MODEL_1_0_URI, "archiveInitiatedBy");
 	static final QName PROP_RETRIEVAL_INITIATED_BY = QName.createQName(GLACIER_ARCHIVE_MODEL_1_0_URI, "retrievalInitiatedBy");
 	static final QName PROP_ARCHIVE_STATUS = QName.createQName(GLACIER_ARCHIVE_MODEL_1_0_URI, "archiveStatus");
 	static final QName PROP_RETRIEVAL_STATUS = QName.createQName(GLACIER_ARCHIVE_MODEL_1_0_URI, "retrievalStatus");
 	static final QName PROP_RETRIEVAL_JOB_ID = QName.createQName(GLACIER_ARCHIVE_MODEL_1_0_URI, "retrievalJobId");

 	static enum ArchiveStatus { INPROGRESS, FAILED, ARCHIVED }
 	static enum RetrievalStatus { INPROGRESS, FAILED, RETRIEVED, RESTORED }
 }
