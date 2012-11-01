package gov.nist.toolkit.securityCommon;

import gov.nist.toolkit.xdsexception.EnvironmentNotSelectedException;

import java.io.File;
import java.io.IOException;

public interface SecurityParams {
	File getCodesFile();
	File getKeystore() throws EnvironmentNotSelectedException;
	String getKeystorePassword() throws IOException, EnvironmentNotSelectedException;
	
	public File getKeystoreDir() throws EnvironmentNotSelectedException;
}
