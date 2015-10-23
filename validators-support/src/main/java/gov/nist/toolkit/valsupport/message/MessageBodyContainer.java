package gov.nist.toolkit.valsupport.message;

import gov.nist.toolkit.errorrecording.ErrorRecorder;
import gov.nist.toolkit.valsupport.client.ValidationContext;
import gov.nist.toolkit.valsupport.engine.MessageValidatorEngine;
import org.apache.axiom.om.OMElement;

/**
 * Created by bill on 6/19/15.
 */
public class MessageBodyContainer extends AbstractMessageValidator {
    OMElement body;

    public MessageBodyContainer(ValidationContext vc, OMElement body) {
        super(vc);
        this.body = body;
    }

    @Override
    public void run(ErrorRecorder er, MessageValidatorEngine mvc) {
        er.registerValidator(this);
        er.unRegisterValidator(this);
    }

    public OMElement getBody() {
        return body;
    }

}
