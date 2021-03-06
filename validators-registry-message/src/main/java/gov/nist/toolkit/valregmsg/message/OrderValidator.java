package gov.nist.toolkit.valregmsg.message;

import gov.nist.toolkit.errorrecording.ErrorRecorder;
import gov.nist.toolkit.errorrecording.client.XdsErrorCode;
import gov.nist.toolkit.registrymetadata.Metadata;
import gov.nist.toolkit.valsupport.client.ValidationContext;
import gov.nist.toolkit.valsupport.engine.MessageValidatorEngine;
import gov.nist.toolkit.valsupport.message.MessageBodyContainer;
import gov.nist.toolkit.valsupport.message.AbstractMessageValidator;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An abstract class that performs XML Element order checking.  The
 * classes build on top of this class supply the element names.
 * @author bill
 *
 */
public abstract class OrderValidator extends AbstractMessageValidator {
	OMElement xml = null;
	protected List<String> elementOrder = new ArrayList<String>();
	String reference;

	abstract protected void initElementOrder();

	public OrderValidator(ValidationContext vc) {
		super(vc);
	}

	/**
	 * Normally this pulls the message body off the validation stack.  But in some cases it
	 * needs to be manually injected through the setBody method.
	 * @param er
	 * @param mvc
	 */
	@Override
	public void run(ErrorRecorder er, MessageValidatorEngine mvc) {
		this.er = er;
		er.registerValidator(this);
		if (xml == null) {
			MessageBodyContainer cont = (MessageBodyContainer) mvc.findMessageValidator("MessageBodyContainer");
			xml = cont.getBody();
		}

		if (xml == null) {
			er.err(XdsErrorCode.Code.XDSRegistryError, "No content present", this, "");
            er.unRegisterValidator(this);
			return;
		}

		checkElementOrder(xml);
        er.unRegisterValidator(this);
	}

	public void setBody(OMElement xml) {
		this.xml = xml;
	}

	void init(String reference) {
		this.reference = reference;
		initElementOrder();
	}

	@SuppressWarnings("unchecked")
	void checkElementOrder(OMElement ele) {
		if (ele == null)
			return;
		for (Iterator<OMElement> it = ele.getChildElements(); it.hasNext(); ) {
			OMElement ele1 = it.next();
			String ele1Name = ele1.getLocalName();
			OMElement ele2 = getNextOMElementSibling(ele1);
			if (ele2 != null) {
				String ele2Name = ele2.getLocalName();
				if (!canFollow(ele1Name, ele2Name))
					er.err(XdsErrorCode.Code.XDSRegistryError, 
							"Child elements of " + ele.getLocalName() + "(id=" + new Metadata().getId(ele) + ")" +
							" are out of Schema required order:   " +
							" element " + ele2.getLocalName() + " cannot follow element " + ele1.getLocalName() + 
							". Elements must be in this order " + elementOrder
							, this, reference);
			}
			checkElementOrder(ele1);
		}
	}

	OMElement getNextOMElementSibling(OMElement ele) {
		OMNode n = null;
		for (n = ele.getNextOMSibling(); n != null && !(n instanceof OMElement); n = n.getNextOMSibling())
			;
		return (OMElement) n; 
	}

	boolean canFollow(String element, String nextElement) {
		if (element == null || element.equals(""))
			return false;
		if (nextElement == null || nextElement.equals(""))
			return false;
		int elementI = elementOrder.indexOf(element);
		int nextElementI = elementOrder.indexOf(nextElement);
		return elementI == -1 || nextElementI == -1 || elementI <= nextElementI;
	}


}
