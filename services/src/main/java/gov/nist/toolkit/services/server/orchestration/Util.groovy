package gov.nist.toolkit.services.server.orchestration

import gov.nist.toolkit.configDatatypes.client.Pid
import gov.nist.toolkit.results.client.Result
import gov.nist.toolkit.results.client.SiteSpec
import gov.nist.toolkit.results.client.TestInstance
import gov.nist.toolkit.services.server.ToolkitApi
import groovy.transform.TypeChecked
/**
 *
 */
@TypeChecked
class Util {
    ToolkitApi api

    public Util(ToolkitApi api) {
        this.api = api
    }

    public void submit(String userName, SiteSpec site, TestInstance testId, String section, Pid patientId, String home) {
        // load the reg/rep with two documents
        List<String> sections = [ section ]
        Map<String, String> qparams = new HashMap<>()
        qparams.put('$patientid$', patientId.asString())
        qparams.put('$testdata_home$', home);

        List<Result> results = api.runTest(userName, site.name, testId, sections, qparams, true)
        if (!results.get(0).passed())
            throw new Exception(results.get(0).toString())
    }

    public void submit(String userName, SiteSpec site, TestInstance testId, String section, Map<String, String> parameters) {
        // load the reg/rep with two documents
        List<String> sections = [ section ]
        List<Result> results = api.runTest(userName, site.name, testId, sections, parameters, true)
        if (!results.get(0).passed())
            throw new Exception(results.get(0).toString())
    }
}
