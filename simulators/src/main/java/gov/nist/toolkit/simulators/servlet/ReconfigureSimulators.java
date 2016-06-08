package gov.nist.toolkit.simulators.servlet;

import gov.nist.toolkit.actorfactory.GenericSimulatorFactory;
import gov.nist.toolkit.actorfactory.SimDb;
import gov.nist.toolkit.actorfactory.client.SimId;
import gov.nist.toolkit.actorfactory.client.SimulatorConfig;
import gov.nist.toolkit.actortransaction.EndpointParser;
import gov.nist.toolkit.configDatatypes.SimulatorProperties;
import gov.nist.toolkit.installation.Installation;
import gov.nist.toolkit.simcommon.client.config.SimulatorConfigElement;
import gov.nist.toolkit.xdsexception.ExceptionUtil;
import org.apache.log4j.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;

/**
 * Reconfigure simulators based on updates to
 *   Toolkit Host
 *   Toolkit Port
 *   Toolkit TLS Port
 */
public class ReconfigureSimulators extends HttpServlet {
    private SimDb db;
    private String configuredHost;
    private String configuredPort;
    private String configuredTlsPort;

    // These are used for testing only
    private String overrideHost = null;
    private String overridePort = null;
    private String overrideTlsPort = null;

    private static Logger logger = Logger.getLogger(ReconfigureSimulators.class);

    public void init(ServletConfig sConfig) {
        configuredHost = Installation.installation().propertyServiceManager().getToolkitHost();
        configuredPort = Installation.installation().propertyServiceManager().getToolkitPort();
        configuredTlsPort = Installation.installation().propertyServiceManager().getToolkitTlsPort();

        db = new SimDb();
        for (SimId simId : db.getAllSimIds()) {
            reconfigure(simId);
        }
    }

    public void reconfigure(SimId simId) {
        boolean error = false;
        boolean updated = false;
        SimulatorConfig config;
        logger.info("Reconfiguring Simulator " + simId.toString());
        try {
            config = db.getSimulator(simId);
        } catch (Exception e) {
            logger.error("    Cannot load " + ExceptionUtil.exception_details(e, 5));
            return;
        }

        for (SimulatorConfigElement ele : config.getEndpointConfigs()) {
            boolean isTls = SimulatorProperties.isTlsEndpoint(ele.getName());
            String existingEndpoint = ele.asString();
            EndpointParser ep = new EndpointParser(existingEndpoint);
            if (!ep.validate()) {
                error = true;
                logger.error("    " + ele.getName() + ": " + existingEndpoint + " - does not validate - " + ep.getError());
                continue;
            }

            String host = ep.getHost();
            String port = ep.getPort();

            if (isTls) {
                if (!host.equals(getConfiguredHost()) || !port.equals(getConfiguredTlsPort())) {
                    ep.updateHostAndPort(getConfiguredHost(), getConfiguredTlsPort());
                    ele.setValue(ep.getEndpoint());
                    updated = true;
                }
            } else {
                if (!host.equals(getConfiguredHost()) || !port.equals(getConfiguredPort())) {
                    ep.updateHostAndPort(getConfiguredHost(), getConfiguredPort());
                    ele.setValue(ep.getEndpoint());
                    updated = true;
                }
            }
        }

        try {
            if (updated)
                new GenericSimulatorFactory(null).saveConfiguration(config);
        } catch (Exception e) {
            logger.error("    Error saving updates: " + e.getMessage());
        }

        if (!error && !updated)
            logger.info("    ok");
        if (!error && updated)
            logger.info("    updated");
    }

    public void setOverrideHost(String overrideHost) {
        this.overrideHost = overrideHost;
    }

    public void setOverridePort(String overridePort) {
        this.overridePort = overridePort;
    }

    public void setOverrideTlsPort(String overrideTlsPort) {
        this.overrideTlsPort = overrideTlsPort;
    }

    public String getConfiguredHost() {
        if (overrideHost != null) return overrideHost;
        return configuredHost;
    }

    public String getConfiguredPort() {
        if (overridePort != null) return overridePort;
        return configuredPort;
    }

    public String getConfiguredTlsPort() {
        if (overrideTlsPort != null) return overrideTlsPort;
        return configuredTlsPort;
    }
}
