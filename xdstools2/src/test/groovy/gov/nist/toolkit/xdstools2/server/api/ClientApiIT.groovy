package gov.nist.toolkit.xdstools2.server.api

import gov.nist.toolkit.actorfactory.SimDb
import gov.nist.toolkit.actorfactory.SimManager
import gov.nist.toolkit.actorfactory.client.Simulator
import gov.nist.toolkit.session.server.Session
import gov.nist.toolkit.sitemanagement.client.Site
import gov.nist.toolkit.testengine.transactions.CallType
import spock.lang.Specification
/**
 *
 * These tests create simulators by generating the necessary files in
 * the configured external cache.  The external cache location
 * and port number are expected to match A RUNNING COPY of toolkit
 * installed on localhost.  We create the simulators but that
 * copy of toolkit runs them. That's why the external cache
 * and port number must agree.
 *
 * Also, before this can be run, the package phase of XDS Toolkit (xdstools2) must
 * be run to setup the target directory.
 *
 * Created by bill on 6/15/15.
 */
class ClientApiIT extends Specification {
    ClientApi client
    Session session
    SimulatorApi simApi
    String regSimId = 'myreg'
    String rrSimId = 'rr'
    boolean tls = false
    String pid = '123^^^&1.2.343&ISO'

    def setup() {
        client = new ClientApi()
        session = client.getSession()
        simApi = new SimulatorApi(session)
    }

    def 'Run 11990 Register test'() {
        setup:
        Simulator sim = simApi.create('reg', regSimId)

        when: 'Create site for simulator'
        Site site = SimManager.getSite(sim.configs.get(0))

        then: 'site exists'
        site

        when: 'Send transaction'
        Map<String, String> parms  = new HashMap<String, String>();
        parms.put('$patientid$', pid);

        boolean status = client.runTest('11990', site, tls, parms, false, CallType.SOAP)

        then:
        status
    }

    def 'Run Provide and Register Transaction to RR'() {
        setup:
        Simulator sim = simApi.create('rr', rrSimId)

        when: 'Create site for simulator'
        Site site = SimManager.getSite(sim.configs.get(0))

        then: 'site exists'
        site

        when: 'Send transaction'
        Map<String, String> parms  = new HashMap<String, String>();
        parms.put('$patientid$', pid);

        boolean status = client.runTest('11966', site, tls, parms, false, CallType.SOAP)

        then:
        status
    }

    def 'Run PnR/SQ/Ret to RR'() {
        setup:
        Simulator sim = simApi.create('rr', rrSimId)

        when: 'Create site for simulator'
        Site site = SimManager.getSite(sim.configs.get(0))

        then: 'site exists'
        site

        when: 'Send transaction'
        Map<String, String> parms  = new HashMap<String, String>();
        parms.put('$patientid$', pid);

        boolean status = client.runTest('12029', site, tls, parms, true, CallType.SOAP)

        then:
        status
    }

    // Set aside because of
    // DocumentEntry(Document01) has size slot with value 36 which disagrees with computed value of 34
//    def 'Run Repository test collection'() {
//        setup:
//        // Build RR sim as target of submission
//        SimulatorApi simApi = new SimulatorApi(session)
//        Simulator sim = simApi.create('rr', rrSimId)
//
//        when: 'Create site for simulator'
//        Site site = SimManager.getSite(sim.configs.get(0))
//
//        then: 'site exists'
//        site
//
//        when: 'Build test client to ack as Repository to send submission'
//        ClientApi client = new ClientApi(session)
//
//        and: 'Send transaction'
//        Map<String, String> parms  = new HashMap<String, String>();
//        parms.put('$patientid$', '123^^^&1.2.343&ISO');
//
//        boolean status = client.runTestCollection('PR.b', site, tls, parms, true)
//
//        then:
//        status
//    }


    // Removed the following from test copy of R.b.tc
    // 12379/no_support disabled
    // Fails because of
    // 11996   -- removed from R.b.tc for now
    // Did not find expected string in error messages: XDSUnknownPatientId
    // This need to be fixed by implementing Registry PID management
    // 11998 - uses alternate patient id
    // 12002 - more pid stuff
    def 'Run Registry test collection'() {
        setup:
        Simulator sim = simApi.create('rr', rrSimId)

        when: 'Create site for simulator'
        Site site = SimManager.getSite(sim.configs.get(0))

        then: 'site exists'
        site

        when: 'Send transaction'
        Map<String, String> parms  = new HashMap<String, String>();
        parms.put('$patientid$', pid);

        boolean status = client.runTestCollection('R.b', site, tls, parms, true, CallType.SOAP)

        then:
        status
    }


    // First of two tests fails - overall call must fail
    def 'Run collection including 11996'() {
        setup:
        Simulator sim = simApi.create('rr', rrSimId)

        when: 'Create site for simulator'
        Site site = SimManager.getSite(sim.configs.get(0))

        then: 'site exists'
        site

        when: 'Send transaction'
        Map<String, String> parms  = new HashMap<String, String>();
        parms.put('$patientid$', pid);

        boolean status = client.runTestCollection('A', site, tls, parms, false, CallType.SOAP)

        then:
        !status
    }

    // Fails because registry does not implement pid management
    def 'Run 11996'() {
        setup:
        Simulator sim = simApi.create('rr', rrSimId)

        when: 'Create site for simulator'
        Site site = SimManager.getSite(sim.configs.get(0))

        then: 'site exists'
        site

        when: 'Send transaction'
        Map<String, String> parms  = new HashMap<String, String>();
        parms.put('$patientid$', pid);

        boolean status = client.runTest('11996', site, tls, parms, false, CallType.SOAP)

        then:
        !status
    }

    def 'Initialize SQ tests'() {
        setup:
        simApi.delete(regSimId)   // Delete sim since old data will mess up results
        Simulator sim = simApi.create('reg', regSimId)

        when: 'Create site for simulator'
        Site site = SimManager.getSite(sim.configs.get(0))

        then: 'site exists'
        site

        when: 'Declare patientid'
        Map<String, String> parms  = new HashMap<String, String>();
        parms.put('$patientid$', pid);

        then:
        true

        when: 'Test data part 1'
        boolean status = client.runTest('12346', site, tls, parms, true, CallType.SOAP)

        then:
        status

        when: 'Test data part 2'
        status = client.runTest('12374', site, tls, parms, true, CallType.SOAP)

        then:
        status
    }

    def 'Run SQ tests'() {
        setup:
        Simulator sim = simApi.create('reg', regSimId)

        when: 'Create site for simulator'
        Site site = SimManager.getSite(sim.configs.get(0))

        then: 'site exists'
        site

        when: 'Run SQ tests'
        Map<String, String> parms  = new HashMap<String, String>();
        parms.put('$patientid$', pid);
        boolean status = client.runTestCollection('SQ.b', site, tls, parms, true, CallType.SOAP)

        then:
        status
    }

    def 'Run GetAll tests'() {
        setup:
        Simulator sim = simApi.create('reg', regSimId)

        when: 'Create site for simulator'
        Site site = SimManager.getSite(sim.configs.get(0))

        then: 'site exists'
        site

        when: 'Run SQ tests'
        Map<String, String> parms  = new HashMap<String, String>();
//        parms.put('$patientid$', pid);
        boolean status = client.runTest('15803', site, tls, parms, true, CallType.SOAP)

        then:
        status
    }

    def 'Run GetAll tests local'() {
        setup:
        Simulator sim = simApi.create('reg', regSimId)
        SimDb db = new SimDb(new File("/Users/bill/tmp/toolkit2/simdb"), regSimId, "Registry", "Register")

        when: 'Create site for simulator'
        Site site = SimManager.getSite(sim.configs.get(0))

        then: 'site exists'
        site

        when: 'Run SQ tests'
        Map<String, String> parms  = new HashMap<String, String>();
//        parms.put('$patientid$', pid);
        boolean status = client.runTest('15803', site, tls, parms, true, CallType.SOAP)

        then:
        status
    }
}
