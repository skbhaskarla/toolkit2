package gov.nist.toolkit.results.client;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

public class AssertionResults implements IsSerializable, Serializable {

	private static final long serialVersionUID = 1L;
	public List<AssertionResult> assertions;
	
	public AssertionResults() {
		assertions = new ArrayList<AssertionResult>();
	}
	
	public AssertionResults(String assertion, boolean status) {
		assertions = new ArrayList<AssertionResult>();
		assertions.add(new AssertionResult(assertion, status));
	}
	
	/* (non-Javadoc)
	 * @see gov.nist.registry.xdstools2.client.AssertionResultsInterface#add(java.lang.String, java.lang.String)
	 */
	public void add(String assertion, String info) {
		assertions.add(new AssertionResult(assertion, info));
	}
	
	/* (non-Javadoc)
	 * @see gov.nist.registry.xdstools2.client.AssertionResultsInterface#add(java.lang.String)
	 */
	public void add(String assertion) {
		assertions.add(new AssertionResult(assertion));
	}
	
	/* (non-Javadoc)
	 * @see gov.nist.registry.xdstools2.client.AssertionResultsInterface#add(java.lang.String, java.lang.String, boolean)
	 */
	public void add(String assertion, String info, boolean status) {
		assertions.add(new AssertionResult(assertion, info, status));
	}
	
	/* (non-Javadoc)
	 * @see gov.nist.registry.xdstools2.client.AssertionResultsInterface#add(java.lang.String, boolean)
	 */
	public void add(String assertion, boolean status) {
		assertion = assertion.replaceAll("\n", "<br />");
		assertions.add(new AssertionResult(assertion, status));
	}
	
	public List<AssertionResult> getAssertions() {
		return assertions;
	}
	
	public boolean isFailed() {
		for (AssertionResult ar : assertions) {
			if (!ar.status)
				return true;
		}
		return false;
	}
	
}
