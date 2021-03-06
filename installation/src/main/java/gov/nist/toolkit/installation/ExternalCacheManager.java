package gov.nist.toolkit.installation;

import gov.nist.toolkit.xdsexception.XdsException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Initialize External Cache
 */
public class ExternalCacheManager {
    static Logger logger = Logger.getLogger(ExternalCacheManager.class);

    synchronized public static String initialize(File location) throws XdsException {
        logger.info("Initialize External Cache to " + location);
        if (!location.exists()) return String.format("External Cache location %s does not exist", location);
        if (!location.isDirectory()) return String.format("External Cache location %s is not a directory", location);
        if (!location.canWrite()) return String.format("External Cache location %s cannot be written", location);
        if (Installation.installation().externalCache() == null)
            Installation.installation().externalCache(location);
        // initialize environment
        initializeDefaultEnvironment(location, Installation.installation().environmentFile());
        return null;
    }

    synchronized public static void reinitialize(File location) throws XdsException {
        logger.info("Reinitialize External Cache to " + location);
        Installation.installation().externalCache(null);
        String error = initialize(location);
        if (error != null) throw new XdsException(error, "");
        File environment = Installation.installation().environmentFile();
        // initialize environment
        initializeDefaultEnvironment(location, environment);
        // initialize test log cache
        Installation.installation().testLogCache().mkdirs();
        // initialize SimDb
        Installation.installation().simDbFile().mkdirs();
    }

    private static void initializeDefaultEnvironment(File location, File environment) throws XdsException {
        logger.info("initialize default environment check");
        if (!environment.exists() || !Installation.installation().environmentFile(Installation.DEFAULT_ENVIRONMENT_NAME).exists()) {
            logger.info("Initializing environments in " + location);
            try {
                FileUtils.copyDirectory(Installation.installation().internalEnvironmentsFile(), new File(location, "environment"));
            } catch (IOException e) {
                throw new XdsException("Cannot initialize environments area of External Cache at " + location, "", e);
            }
        }
    }

    public static void initializeFromMarkerFile(File markerFile) throws XdsException {
        reinitialize(markerFile.getParentFile());
    }

    public static void initialize() throws XdsException {
        File location = new File(Installation.installation().propertyServiceManager().getPropertyManager().getExternalCache());
        initialize(location);
    }

    public static String validate() {
        File location = Installation.installation().externalCache();
        if (!location.exists()) return String.format("External Cache location %s does not exist. " + HOW_TO_FIX, location);
        if (!location.isDirectory()) return String.format("External Cache location %s is not a directory. " + HOW_TO_FIX, location);
        if (!location.canWrite()) return String.format("External Cache location %s cannot be written. " + HOW_TO_FIX, location);
        File defEnv = Installation.installation().environmentFile("default");
        if (!defEnv.exists()) return String.format("Default Environment (default) not found in External Cache (%s). " +
                        HOW_TO_FIX,
                location);


        return null;
    }

    static final String HOW_TO_FIX = "Open Toolkit Configuration, edit External Cache location (if necessary) and save. If your External Cache location is ok " +
            " you may only need to update your External Cache.  The SAVE will do that update.";
}
