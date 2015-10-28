package gov.nist.toolkit.valregmetadata.object;

import gov.nist.toolkit.errorrecording.ErrorRecorder;
import gov.nist.toolkit.registrymetadata.Metadata;
import gov.nist.toolkit.valsupport.client.ValidationContext;
import gov.nist.toolkit.xdsexception.XdsInternalException;

import org.apache.axiom.om.OMElement;

public class RegistryObject extends AbstractRegistryObject {

	public RegistryObject(Metadata m, OMElement ro) throws XdsInternalException  {
		super(m, ro);
	}

	@Override
	public String identifyingString() {
		return null;
	}

	@Override
	public OMElement toXml()  {
		return null;
	}

	@Override
	public void validateRequiredSlotsPresent(ErrorRecorder er,
			ValidationContext vc) {

	}

	@Override
	public void validateSlotsCodedCorrectly(ErrorRecorder er,
			ValidationContext vc) {

	}

	@Override
	public void validateSlotsLegal(ErrorRecorder er) {

	}

}
