package gov.nist.toolkit.xdstools2.server.api

import gov.nist.toolkit.installation.Installation
import gov.nist.toolkit.results.client.Result
import spock.lang.Specification
/**
 * Runs all Registry tests.
 * To run:
 *    Start toolkit from IntelliJ.  This will establish the EC at a location something like
 *       /Users/bill/dev/toolkit2/xdstools2/target/test-classes/external_cache
 *    On startup this will be echoed in the log window
 *    Using the toolkit [Toolkit Configuration] tool, save this location as the external cache location
 *    Shutdown toolkit
 *    Start toolkit - this will recognize the EC location change
 *    Open Simulation Manager
 *    Select test session named mike (create it if it doesn't exist)
 *    Create a Registry simulator named reg - the full id will be mike__reg
 *    Come back to this file in IntelliJ and click right on the class name and select Run RegistrySelfTestIT
 *    All the self tests will run
 */
class RegistrySelfTestIT extends Specification {
    ToolkitApi api;
    String patientId = 'BR14^^^&1.2.360&ISO'
    String reg = 'mike__reg'
    String regrep = 'mike__regrep'

    def setup() {
//        Installation.installation().externalCache(new File('/Users/bill/tmp/toolkit2'))
        api = new ToolkitApi()
        println "EC is ${Installation.installation().externalCache().toString()}"
        println "${api.getSiteNames(true)}"
    }

    // submits the patient id configured above to the registry in a Patient Identity Feed transaction
    def 'Submit Pid transaction to Registry simulator'() {
        when:
        String testSession = null;  // use default
        String siteName = 'mike__reg'
        String testName = "15804"
        List<String> sections = new ArrayList<>()
        sections.add("section")
        Map<String, String> params = new HashMap<>()
        params.put('$patientid$', patientId)
        boolean stopOnFirstError = true

        and: 'Run pid transaction test'
        List<Result> results = api.runTest(testSession, siteName, testName, sections, params, stopOnFirstError)

        then:
        true
        results.size() == 1
        results.get(0).passed()
    }



//    def 'Run 11990'() {
//        when:
//        String testSession = null;  // use default
//        String siteName = 'mike__reg'
//        String testName = "11990"
//        List<String> sections = new ArrayList<>()
//        Map<String, String> params = new HashMap<>()
//        params.put('$patientid$', patientId)
//        boolean stopOnFirstError = true
//
//        and: 'Run Register test'
//        List<Result> results = api.runTest(testSession, siteName, testName, sections, params, stopOnFirstError)
//
//        then:
//        true
//        results.size() == 1
//        results.get(0).passed()
//    }

//    def 'Run test under debug'() {
//        when:
//        String testSession = null;  // use default
//        String siteName = 'mike__reg'
//        String testName = "12002"
//        List<String> sections = new ArrayList<>()
//        Map<String, String> params = new HashMap<>()
//        params.put('$patientid$', patientId)
//        boolean stopOnFirstError = true
//
//        and: 'Run Register test'
//        List<Result> results = api.runTest(testSession, siteName, testName, sections, params, stopOnFirstError)
//
//        then:
//        true
//        results.size() == 1
//        results.get(0).passed()
//    }

    def 'Run all Register tests'() {
        when:
        String testSession = null;  // use default
        String siteName = 'mike__reg'
        String testName = "tc:R.b"
        List<String> sections = new ArrayList<>()
        Map<String, String> params = new HashMap<>()
        params.put('$patientid$', patientId)
        boolean stopOnFirstError = true

        and: 'Run'
        List<Result> results = api.runTest(testSession, siteName, testName, sections, params, stopOnFirstError)

        then:
        true
        results.size() == 1
        results.get(0).passed()
    }

    def 'Run SQ initialization'() {
        when:
        String testSession = null;  // use default
        String siteName = 'mike__reg'
        String testName = "tc:Initialize_for_Stored_Query"
        List<String> sections = new ArrayList<>()
        Map<String, String> params = new HashMap<>()
        params.put('$patientid$', patientId)
        boolean stopOnFirstError = true

        and: 'Run'
        List<Result> results = api.runTest(testSession, siteName, testName, sections, params, stopOnFirstError)

        then:
        true
        results.size() == 1
        results.get(0).passed()
    }

    def 'Run SQ tests'() {
        when:
        String testSession = null;  // use default
        String siteName = 'mike__reg'
        String testName = "tc:SQ.b"
        List<String> sections = new ArrayList<>()
        Map<String, String> params = new HashMap<>()
        params.put('$patientid$', patientId)   // not used
        boolean stopOnFirstError = true

        and: 'Run'
        List<Result> results = api.runTest(testSession, siteName, testName, sections, params, stopOnFirstError)

        then:
        true
        results.size() == 1
        results.get(0).passed()
    }

//    def 'Run all PnR tests'() {
//        when:
//        String testSession = null;  // use default
//        String siteName = 'mike__regrep'
//        String testName = "tc:PR.b"
//        List<String> sections = new ArrayList<>()
//        Map<String, String> params = new HashMap<>()
//        params.put('$patientid$', patientId)
//        boolean stopOnFirstError = true
//
//        and: 'Run'
//        List<Result> results = api.runTest(testSession, siteName, testName, sections, params, stopOnFirstError)
//
//        then:
//        true
//        results.size() == 1
//        results.get(0).passed()
//    }

}