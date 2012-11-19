package gov.nist.toolkit.testengine;

import gov.nist.toolkit.results.client.SiteSpec;
import gov.nist.toolkit.securityCommon.SecurityParams;

import java.io.File;

public class TransactionSettings {
	/**
	 * Should Patient ID be assigned from xdstest config on Submissions?
	 */
	public Boolean assignPatientId = null;   // allows for null (unknown)
	public String patientId = null;
	public File logDir = null;
	public boolean writeLogs = false;
	public SiteSpec siteSpec;
	public File toolkitx;
	
	public SecurityParams securityParams = null;
}
