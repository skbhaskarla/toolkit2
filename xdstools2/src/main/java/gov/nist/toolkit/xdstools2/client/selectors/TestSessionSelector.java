package gov.nist.toolkit.xdstools2.client.selectors;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.*;
import gov.nist.toolkit.xdstools2.client.Xdstools2;
import gov.nist.toolkit.xdstools2.client.event.testSession.TestSessionChangedEvent;
import gov.nist.toolkit.xdstools2.client.event.testSession.TestSessionChangedEventHandler;
import gov.nist.toolkit.xdstools2.client.event.testSession.TestSessionsUpdatedEvent;
import gov.nist.toolkit.xdstools2.client.event.testSession.TestSessionsUpdatedEventHandler;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class TestSessionSelector {
    ListBox listBox = new ListBox();
    TextBox textBox = new TextBox();
    HorizontalPanel panel;
    static final String NONSELECTION = "--Select--";

    public TestSessionSelector(List<String> initialContents, String initialSelection) {
//        Xdstools2.DEBUG("initialize TestSessionSelector with " + initialContents + " ==>" + initialSelection);
        build(initialContents, initialSelection);
        link();
    }

    // Listen on the EventBus in the future
    private void link() {

        // Test sessions reloaded
        Xdstools2.getEventBus().addHandler(TestSessionsUpdatedEvent.TYPE, new TestSessionsUpdatedEventHandler() {
            @Override
            public void onTestSessionsUpdated(TestSessionsUpdatedEvent event) {
                listBox.clear();
                for (String i : event.testSessionNames) listBox.addItem(i);
            }
        });


        // SELECT
        Xdstools2.getEventBus().addHandler(TestSessionChangedEvent.TYPE, new TestSessionChangedEventHandler() {
            @Override
            public void onTestSessionChanged(TestSessionChangedEvent event) {
                if (event.changeType == TestSessionChangedEvent.ChangeType.SELECT) {
                    listBox.setSelectedIndex(indexOfValue(event.value));
                }
            }
        });
    }

    // Initialize screen now
    private void build(List<String> initialContents, String initialSelection) {
        if (initialContents == null) initialContents = new ArrayList<>();
        List<String> contents = new ArrayList<>();

        contents.add(NONSELECTION);
        contents.addAll(initialContents);


        panel = new HorizontalPanel();

        panel.add(new HTML("Test Session: "));

        //
        // List Box
        //
        for (String i : contents) listBox.addItem(i);
        if (contents.contains(initialSelection))
            listBox.setSelectedIndex(contents.indexOf(initialSelection));
        else
            listBox.setSelectedIndex(0);
        panel.add(listBox);
        listBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                String newValue = listBox.getValue(listBox.getSelectedIndex());
                if (NONSELECTION.equals(newValue)) return;
                Xdstools2.getTestSessionManager().setCurrentTestSession(newValue);
                Xdstools2.getEventBus().fireEvent(new TestSessionChangedEvent(TestSessionChangedEvent.ChangeType.SELECT, newValue));
            }
        });

        panel.add(textBox);
        textBox.addKeyPressHandler(new KeyPressHandler()
        {
            @Override
            public void onKeyPress(KeyPressEvent event_)
            {
                boolean enterPressed = KeyCodes.KEY_ENTER == event_
                        .getNativeEvent().getKeyCode();
                if (enterPressed) {
                    add();
                }
            }
        });

        //
        // Add Button
        //
        Button addTestSessionButton = new Button("Add");
        panel.add(addTestSessionButton);
        addTestSessionButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                String value = textBox.getValue().trim();
                textBox.setValue("");
                if ("".equals(value)) return;
                Xdstools2.getEventBus().fireEvent(new TestSessionChangedEvent(TestSessionChangedEvent.ChangeType.ADD, value));
            }
        });

        //
        // Delete Button
        //
        Button delTestSessionButton = new Button("Delete");
        panel.add(delTestSessionButton);
        delTestSessionButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                String value = listBox.getValue(listBox.getSelectedIndex());
                Xdstools2.getEventBus().fireEvent(new TestSessionChangedEvent(TestSessionChangedEvent.ChangeType.DELETE, value));
            }
        });
    }

    void add() {
        String value = textBox.getValue().trim();
        value = value.replaceAll(" ", "_");
        textBox.setValue("");
        if ("".equals(value)) return;
        Xdstools2.getEventBus().fireEvent(new TestSessionChangedEvent(TestSessionChangedEvent.ChangeType.ADD, value));
    }

    public Widget asWidget() { return panel; }

    int indexOfValue(String value) {
        for (int i=0; i<listBox.getItemCount(); i++)
            if (listBox.getItemText(i).equals(value))
                return i;
        return -1;
    }
}
