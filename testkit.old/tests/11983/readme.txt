PnR.b Reject submissions where metadata and documents do not match

The submit and eval directories hold test steps with the same names, 
shown below.  The submit tests test that the proper error is returned.  
The eval tests test that no contents are present in the Registry.

*_no_metadata
	A document is present as attachment but no XDSDocuementEntry is 
present in metadata. The error XDSMissingDocumentMetadata must be returned.

*_no_doc
	A XDSDocumentEntry is present in metadata but no corresponding 
document is attached. The error XDSMissingDocument must be returned.


