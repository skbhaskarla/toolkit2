package gov.nist.toolkit.xdstools2.client.event.testSession;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Can carry one of three changes:
 *  delete
 *  add
 *  selection change
 */
public class TestSessionChangedEvent extends GwtEvent<TestSessionChangedEventHandler> {
    public static final Type<TestSessionChangedEventHandler> TYPE = new Type<>();
    public enum ChangeType {ADD, DELETE, SELECT};
    public ChangeType changeType;
    public String value;

    public TestSessionChangedEvent(ChangeType changeType, String value) {
        this.changeType = changeType;
        this.value = value;
    }

    @Override
    public Type<TestSessionChangedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(TestSessionChangedEventHandler testSessionChangedEventHandler) {
        testSessionChangedEventHandler.onTestSessionChanged(this);
    }
}
