package gov.nist.toolkit.testenginelogging.logrepository;

import gov.nist.toolkit.results.client.TestInstance;
import gov.nist.toolkit.testenginelogging.LogMap;
import gov.nist.toolkit.xdsexception.ExceptionUtil;
import gov.nist.toolkit.xdsexception.XdsException;
import gov.nist.toolkit.xdsexception.XdsInternalException;
import org.apache.log4j.Logger;

import java.io.*;

public class JavaSerializationIO implements ILoggerIO  {
	Logger logger = Logger.getLogger(JavaSerializationIO.class);

	/* (non-Javadoc)
	 * @see gov.nist.toolkit.testenginelogging.logrepository.ILoggerIO#logOut(gov.nist.toolkit.results.client.XdstestLogId, gov.nist.toolkit.testengine.LogMap, java.io.File)
	 */
	@Override
	public void logOut(TestInstance id, LogMap log, File logDir) throws XdsException {
		logger.debug("Writing logs " + log.getKeys() + " to " + logFile(id, logDir));
		FileOutputStream fos;
		ObjectOutputStream out = null;
		try {
			fos = new FileOutputStream(logFile(id, logDir));
			out = new ObjectOutputStream(fos);
			out.writeObject(log);
		} catch (IOException e) {
			throw new XdsInternalException("Cannot write transaction log file", e);
		} finally {
			try {
				out.close();
			} catch (IOException e) {
//				throw new XdsInternalException("Cannot write transaction log file", e);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see gov.nist.toolkit.testenginelogging.logrepository.ILoggerIO#logIn(gov.nist.toolkit.results.client.XdstestLogId, java.io.File)
	 */
	@Override
	public LogMap logIn(TestInstance id, File logDir) throws Exception {
		logger.debug("Reading log from " + logFile(id, logDir));
		FileInputStream fis = null;
		ObjectInputStream in = null;
		try {
			fis = new FileInputStream(logFile(id, logDir));
			in = new ObjectInputStream(fis);
			LogMap map = (LogMap) in.readObject();
			logger.debug("restoring log " + map.getKeys() + " from " + logFile(id, logDir));
			return map;
		} 
		catch (ClassNotFoundException e) {
			logger.debug("attempting to restore log " + "from " + logFile(id, logDir));
			throw new XdsInternalException("Cannot create object of type LogMap - class not found", e);
		} catch (Exception e) {
			logger.error(ExceptionUtil.here("Cannot load " + logFile(id, logDir)));
			throw e;
		} finally {
			if (in != null)
				in.close();
		}
	}

	String logFile(TestInstance id, File logDir)  {
		return logDir.toString() + File.separator + id.getId();
	}


}
