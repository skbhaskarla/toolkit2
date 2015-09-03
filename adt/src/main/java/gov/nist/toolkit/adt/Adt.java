package gov.nist.toolkit.adt;

import gov.nist.toolkit.actorfactory.SimDb;
import gov.nist.toolkit.actorfactory.client.NoSimException;
import gov.nist.toolkit.utilities.io.Io;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Created by bill on 9/1/15.
 */
public class Adt {
    static Logger logger = Logger.getLogger(Adt.class);

    static public void addPatientId(String simId, String patientId) throws IOException, NoSimException {
        File pidFile = pidFile(simId, patientId);
        Io.stringToFile(pidFile, patientId);
    }

    static public boolean hasPatientId(String simId, String patientId)  {
        try {
            File pidFile = pidFile(simId, patientId);
            Io.stringFromFile(pidFile);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    static File pidFile(String simId, String patientId) throws IOException, NoSimException {
        logger.debug("patientID is " + patientId);
        SimDb simdb = new SimDb(simId);
        logger.debug("simdir = " + simdb.getSimDir());
        String[] parts = patientId.split("\\^");
        logger.debug("parts.length = " + parts.length);
        if (parts.length != 4)
            return null;   // not valid pid
        String id = parts[0];
        logger.debug("id is " + id);
        String ad = parts[3];
        String[] parts2 = ad.split("&");
        logger.debug("parts2.length = " + parts2.length);
        if (parts2.length < 2)
            return null;
        String oid = parts2[1];
        logger.debug("oid is " + oid);
        File pidFile = simdb.getPidFile(oid, id);
        logger.debug("pidfile is " + pidFile);
        return pidFile;
    }
}
