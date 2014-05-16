glacier-archive-action
======================

##A simple "Archive to Glacier" action for Alfresco.##

**Archiving**

This extension adds a new action to Alfresco and the Share doclib / doc details view.  When fired, the action pushes a single
piece of content to the defined AWS Glacier vault.  Optionally, the content stream for the document is deleted.  The actual
node is preserved so it is still searchable by its metadata, and a new Glacier aspect is applied.  This aspect carries 
properties that contains all of the information requried to retrieve the node.

**Retrieval**

Ultimately we plan to support multiple retrieval options, including direct download from AWS and pulling the document back
into Alfresco.

Feature requests?  Ideas?  Let's hear them!
