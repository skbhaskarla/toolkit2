package gov.nist.toolkit.xdstools2.client.event.testSession;

import com.google.gwt.event.shared.GwtEvent;

import java.util.List;

/**
 * Created by bill on 9/16/15.
 */
public class TestSessionsUpdatedEvent extends GwtEvent<TestSessionsUpdatedEventHandler> {
    public static final Type<TestSessionsUpdatedEventHandler> TYPE = new Type<>();
    public List<String>  testSessionNames;

    public TestSessionsUpdatedEvent(List<String> testSessionNames) {
        this.testSessionNames = testSessionNames;
    }

    @Override
    public Type<TestSessionsUpdatedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(TestSessionsUpdatedEventHandler testSessionsUpdatedEventHandler) {
        testSessionsUpdatedEventHandler.onTestSessionsUpdated(this);
    }
}
