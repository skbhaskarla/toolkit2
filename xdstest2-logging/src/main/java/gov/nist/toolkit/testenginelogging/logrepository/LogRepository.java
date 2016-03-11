package gov.nist.toolkit.testenginelogging.logrepository;

import gov.nist.toolkit.actorfactory.SimDb;
import gov.nist.toolkit.results.client.LogIdIOFormat;
import gov.nist.toolkit.results.client.LogIdType;
import gov.nist.toolkit.results.client.TestInstance;
import gov.nist.toolkit.testenginelogging.LogMap;
import gov.nist.toolkit.xdsexception.ExceptionUtil;
import gov.nist.toolkit.xdsexception.ToolkitRuntimeException;
import gov.nist.toolkit.xdsexception.XdsException;
import org.apache.log4j.Logger;

import java.io.File;

public class LogRepository  {
    static Logger log = Logger.getLogger(LogRepository.class);

    // Both of these are initialized by LogRepositoryFactory
//	File logDir;
    ILoggerIO logger;
    File location;
    String user;
    TestInstance id;
    LogIdIOFormat format;
    LogIdType idType;

    // Create through LogRepositoryFactory only
    LogRepository(File location, String user, LogIdIOFormat format, LogIdType idType, TestInstance id) {
        this.location = location;
        this.user = user;
        this.format = format;
        this.idType = idType;
        this.id = id;

        if (id != null) {
            if (location != null)
                id.setLocation(location.toString());
            id.setUser(user);
            id.setFormat(format);
            id.setIdType(idType);
        }
    }

    public String toString() {
        try {
            String s = logDir().toString();
            return s;
        } catch (Exception e) {
            return "LogRepository: bad log directory";
        }
    }

    public void logOut(TestInstance id, LogMap logMap)
            throws XdsException {
        log.debug(String.format("Saving log for %s", id));
        logger.logOut(id, logMap, logDir(id));
    }

    public void logOutIfLinkedToUser(TestInstance id, LogMap logMap) throws XdsException {
//        if (idType == LogIdType.SPECIFIC_ID)
            logOut(id, logMap);
//        else {
//            log.debug(String.format("Not saving log for %s - not tied to user", id));
//        }
    }

//	public LogMap logIn(TestId id) throws Exception {
//		return logger.logIn(id, logDir());
//	}

    static public LogMap logIn(TestInstance testInstance) throws Exception {
        if (testInstance == null) {
            log.error(ExceptionUtil.here("testId is null"));
            return null;
        }
        try {
            LogRepository repo = LogRepositoryFactory.getRepository(new File(testInstance.getLocation()),
                    testInstance.getUser(),
                    testInstance.getFormat(),
                    testInstance.getIdType(),
                    testInstance);
            File dir = repo.logDir(testInstance);
            log.debug(String.format("Loading LogMap for test %s from %s", testInstance, dir));
            return repo.logger.logIn(testInstance, dir);
        } catch (Exception e) {
            log.error("Cannot load " + testInstance.describe());
            throw e;
        }
    }

//    public LogMap logIn(File logDir) throws Exception {
//        return logger.logIn(id, logDir);
//    }

    public File logDir() {
        File dir = getLogDir(/*location, user, idType,*/ id);
        if (dir.toString().contains("tc:")) throw new ToolkitRuntimeException("Bad LogDir - " + dir);
//        log.debug(ExceptionUtil.here("LogRepository at " + dir));
        return dir;
    }

    public File logDir(TestInstance id) {
        File dir = getLogDir(/*location, user, idType,*/ id);
        if (dir.toString().contains("tc:")) throw new ToolkitRuntimeException("Bad LogDir - " + dir);
//        log.debug(ExceptionUtil.here("LogRepository at " + dir));
        return dir;
    }

    // assign event including filenames - do not touch file system in case the event is never used
    public void assignEvent(TestInstance testInstance) {
        if (idType != LogIdType.TIME_ID) return;  // doesnt use event structure
        if (testInstance.linkedToLogRepository()) return;
        String event = new SimDb().nowAsFilenameBase();
        testInstance.setInternalEvent(event);
        File dir = new File(
                location + File.separator + user +
                        File.separator + event);
        log.debug(String.format("Assigning Event Dir to test instance %s - %s", testInstance, dir));
        testInstance.setEventDir(dir.toString());
        testInstance.setLocation(location.toString());
        testInstance.setUser(user);
        testInstance.setFormat(format);
        testInstance.setIdType(idType);
    }

    // same event - new test id
    static public TestInstance cloneTestInstance(TestInstance testInstance, String newId) {
        TestInstance ti = testInstance.copy();
        ti.setId(newId);
        return ti;
    }

    File getLogDir(/*File location, String user, LogIdType idType,*/ TestInstance id) {
        if (location == null) throw new ToolkitRuntimeException("Internal Error: location is null");
        if (user == null) throw new ToolkitRuntimeException("Internal Error: user is null");
        assignEvent(id);
        if (idType == LogIdType.TIME_ID) {
            File logDir = new File(id.getEventDir());
            logDir.mkdirs();
            if (!logDir.exists())
                throw new ToolkitRuntimeException("Cannot create log directory " + logDir.toString());
            if (!logDir.isDirectory())
                throw new ToolkitRuntimeException("Cannot create log directory " + logDir.toString());
            return logDir;
        } else if (idType == LogIdType.SPECIFIC_ID) {
            File logDir = new File(location, user);
            if (id != null)  // if null then it cannot be used with logDir() call, must use logDir(String)
                logDir = new File(logDir, id.getId());
            logDir.mkdirs();
            if (!logDir.exists())
                throw new ToolkitRuntimeException("Cannot create log directory " + logDir.toString());
            if (!logDir.isDirectory())
                throw new ToolkitRuntimeException("Cannot create log directory " + logDir.toString());
            return logDir;
        }
        return null;
    }

    public String getUser() {
        return user;
    }
}
