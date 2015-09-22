package gov.nist.toolkit.xdstools2.client.tabs.simulatorControlTab;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import gov.nist.toolkit.actorfactory.client.Pid;
import gov.nist.toolkit.actorfactory.client.PidBuilder;
import gov.nist.toolkit.actorfactory.client.SimId;
import gov.nist.toolkit.actorfactory.client.SimulatorConfig;
import gov.nist.toolkit.xdstools2.client.PopupMessage;
import gov.nist.toolkit.xdstools2.client.TabContainer;
import gov.nist.toolkit.xdstools2.client.siteActorManagers.FindDocumentsSiteActorManager;
import gov.nist.toolkit.xdstools2.client.tabs.genericQueryTab.GenericQueryTab;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bill on 9/21/15.
 */
public class PidEditTab extends GenericQueryTab {
    SimulatorConfig config;
    SimId simId;
    ListBox pidList = new ListBox();
    TextArea pidBox = new TextArea();

    public PidEditTab(SimulatorConfig config) {
        super(new FindDocumentsSiteActorManager());
        this.config = config;
        simId = config.getId();
    }

    public void onTabLoad(TabContainer container, boolean select, String eventName) {
        myContainer = container;
        topPanel = new VerticalPanel();

        container.addTab(topPanel, "Pid Edit", select);
        addCloseButton(container, topPanel, null);

        topPanel.add(new HTML("<h2>Patient ID Display/Edit</h2>"));
        topPanel.add(new HTML("<h3>Simulator " + simId.toString() + "</h3>"));

        HorizontalPanel panel = new HorizontalPanel();
        topPanel.add(panel);
        VerticalPanel listPanel = new VerticalPanel();
        panel.add(listPanel);
        listPanel.add(new HTML("Registered Patient IDs"));
        pidList.setVisibleItemCount(25);
        pidList.setMultipleSelect(true);
        listPanel.add(pidList);
        Button deleteButton = new Button("Delete", new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                List<String> toDelete = new ArrayList<>();
                for (int i=0; i<pidList.getItemCount(); i++) {
                    if (pidList.isItemSelected(i)) {
                        toDelete.add(pidList.getItemText(i));
                    }
                }
                if (toDelete.size() == 0) new PopupMessage("Nothing selected to delete");
                else {
                    List<Pid> pidsToDelete = new ArrayList<>();
                    for (String pidString : toDelete) {
                        Pid p = PidBuilder.createPid(pidString);
                        if (p != null) pidsToDelete.add(p);
                    }
                    try {
                        toolkitService.deletePatientIds(simId, pidsToDelete, new AsyncCallback<Boolean>() {
                            @Override
                            public void onFailure(Throwable throwable) {
                                new PopupMessage("Error deleting Patient IDs - " + throwable.getMessage());
                            }

                            @Override
                            public void onSuccess(Boolean aBoolean) {
                                loadPids();
                            }
                        });
                    } catch (Exception e) {
                        new PopupMessage("Error deleting Patient IDs - " + e.getMessage());
                    }
                }
            }
        });
        listPanel.add(deleteButton);

        VerticalPanel pidPanel = new VerticalPanel();
        panel.add(pidPanel);
        pidPanel.add(new HTML("Patient ID(s) to Add..."));
        pidBox.setCharacterWidth(50);
        pidBox.setVisibleLines(20);
        pidPanel.add(pidBox);


        pidPanel.add(new Button("Add", new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                final String value = pidBox.getValue();
                List<String> stringValues = formatIds(value);
                final List<Pid> pids = new ArrayList<>();
                List<String> badPidList = new ArrayList<>();
                for (String stringValue : stringValues) {
                    Pid pid = PidBuilder.createPid(stringValue);
                    if (pid == null) {
                        badPidList.add(stringValue);
                    } else {
                        pids.add(pid);
                    }
                }
                if (badPidList.size() > 0)
                    new PopupMessage("These are not properly formatted Patient IDs, they will be ignored - " + badPidList);
                if (pids.size() == 0) {
                    new PopupMessage("Enter some Patient IDs");
                    clear();
                    return;
                }
                try {
                    toolkitService.addPatientIds(simId, pids, new AsyncCallback<String>() {
                        @Override
                        public void onFailure(Throwable throwable) {
                            new PopupMessage("Error saving Patient ID - " + throwable.getMessage());
                        }

                        @Override
                        public void onSuccess(String o) {
                            int dups = 0;
                            int added = 0;
                            for (Pid pid : pids) {
                                String s = pid.toString();
                                if (contains(s)) { dups++; continue; }
                                added++;
                                pidList.insertItem(s, 0);
                            }
                            clear();
                            if (dups == 0) new PopupMessage(added + " Patient IDs added");
                            else if (dups == 1) new PopupMessage("1 Patient ID added, others were duplicates");
                            else new PopupMessage(added + " Patient IDs added, " + dups + "  were duplicates");
                        }
                    });
                } catch (Exception e) {
                    new PopupMessage("Error saving Patient ID - " + e.getMessage());
                }
            }

            boolean contains(String pidString) {
                for (int i=0; i<pidList.getItemCount(); i++) {
                    if (pidString.equals(pidList.getItemText(i))) return true;
                }
                return false;
            }

            void clear() { pidBox.setText(""); }
        }));

        loadPids();
    }

    private void loadPids() {
        try {
            toolkitService.getPatientIds(simId, new AsyncCallback<List<Pid>>() {
                @Override
                public void onFailure(Throwable throwable) {
                    new PopupMessage("Error retrieving Patient IDs - " + throwable.getMessage());
                }

                @Override
                public void onSuccess(List<Pid> pids) {
                    pidList.clear();
                    for (Pid pid : pids) pidList.addItem(pid.toString());
                }
            });
        } catch (Exception e) {
            new PopupMessage("Error retrieving Patient IDs - " + e.getMessage());
        }
    }

    public String getWindowShortName() {
        return "PidEditTab";
    }
}
