package gov.nist.toolkit.valregmsg.registry.storedquery.validation;

import gov.nist.toolkit.registrymetadata.Metadata;
import gov.nist.toolkit.registrysupport.logging.LoggerException;
import gov.nist.toolkit.valregmsg.registry.storedquery.generic.GetDocuments;
import gov.nist.toolkit.valregmsg.registry.storedquery.support.StoredQuerySupport;
import gov.nist.toolkit.xdsexception.MetadataException;
import gov.nist.toolkit.xdsexception.XdsException;

public class ValidationGetDocuments extends GetDocuments {

	public ValidationGetDocuments(StoredQuerySupport sqs) {
		super(sqs);
	}

	@Override
	protected Metadata runImplementation() throws MetadataException,
			XdsException, LoggerException {
		return null;
	}

}
