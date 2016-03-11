package gov.nist.toolkit.services.server.orchestration

import gov.nist.toolkit.installation.Installation
import gov.nist.toolkit.services.client.IgOrchestrationRequest
import gov.nist.toolkit.services.client.RawResponse
import gov.nist.toolkit.services.client.RgOrchestrationRequest
import gov.nist.toolkit.services.server.RawResponseBuilder
import gov.nist.toolkit.services.server.ToolkitApi
import gov.nist.toolkit.session.server.Session
import groovy.transform.TypeChecked
/**
 *
 */
@TypeChecked
class OrchestrationManager {

    public RawResponse buildIgTestEnvironment(Session session, IgOrchestrationRequest request) {
        try {
            ToolkitApi api
            if(Installation.installation().warHome()) {
                api = ToolkitApi.forNormalUse(session)
            } else {
                api = ToolkitApi.forInternalUse()
            }
            return new IgOrchestrationBuilder(api, session, request).buildTestEnvironment()
        } catch (Exception e) {
            return RawResponseBuilder.build(e);
        }
    }

    public RawResponse buildRgTestEnvironment(Session session, RgOrchestrationRequest request) {
        try {
            ToolkitApi api
            if(Installation.installation().warHome()) {
                api = ToolkitApi.forNormalUse(session)
            } else {
                api = ToolkitApi.forInternalUse()
            }
            return new RgOrchestrationBuilder(api, session, request).buildTestEnvironment()
        } catch (Exception e) {
            return RawResponseBuilder.build(e);
        }
    }


}
