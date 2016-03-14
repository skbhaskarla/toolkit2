package gov.nist.toolkit.xdstools2.client.tabs;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import gov.nist.toolkit.xdstools2.client.*;
import gov.nist.toolkit.xdstools2.client.Panel;
import gov.nist.toolkit.xdstools2.client.selectors.EnvironmentManager;
import gov.nist.toolkit.xdstools2.client.siteActorManagers.NullSiteActorManager;
import gov.nist.toolkit.xdstools2.client.tabs.genericQueryTab.GenericQueryTab;
import gov.nist.toolkit.xdstools2.client.widgets.TestkitConfigTool;

import java.util.Map;

public class ToolConfigTab extends GenericQueryTab {
	
	FlexTable grid = new FlexTable();
	Map<String, String> props;
	Button loadAllGazelleConfigs = new Button("Load all Gazelle configs");
	int gridRow;

	public ToolConfigTab() {
		super(new NullSiteActorManager());
	}

	public void onTabLoad(TabContainer container, boolean select, String eventName) {
		myContainer = container;
		topPanel = new VerticalPanel();


		container.addTab(topPanel, "ToolConfig", select);
		addCloseButton(container,topPanel, null);

		HTML title = new HTML();
		title.setHTML("<h2>Configure XDS Toolkit</h2>");
		topPanel.add(title);

		buildGrid();
	}
	
	class RmOldSimsClickHandler implements ClickHandler {

		public void onClick(ClickEvent event) {
			toolkitService.removeOldSimulators(new AsyncCallback<Integer> () {

				public void onFailure(Throwable caught) {
					new PopupMessage("removeOldSimulators() call failed: " + caught.getMessage());
				}

				public void onSuccess(Integer result) {
					new PopupMessage(result + " simulators removed");
				}
				
			});
		}
		
	}


	// Boolean data type ignored 
	AsyncCallback<Boolean> signedInCallback = new AsyncCallback<Boolean> () {

		public void onFailure(Throwable ignored) {
		}

		public void onSuccess(Boolean ignored) {
			buildGrid();
		}

	};



	void buildGrid() {

		if (PasswordManagement.isSignedIn) {
		}
		else {
			PasswordManagement.addSignInCallback(signedInCallback);

			new AdminPasswordDialogBox(topPanel);

			return;
		}

		loadPropertyFile();

		HTML subtitle1 = new HTML();
		subtitle1.setHTML("<h3>Properties</h3>");
		topPanel.add(subtitle1);

		topPanel.add(grid);

		Button goButton = new Button("Save");
		goButton.addClickHandler(new Saver());
		topPanel.add(goButton);

		HTML separator = new HTML();
		separator.setHTML("<br/>");
		topPanel.add(separator);

		HTML subtitle2 = new HTML();
		subtitle2.setHTML("<br/><br/>");
		topPanel.add(subtitle2);

		topPanel.add(loadAllGazelleConfigs);
		loadAllGazelleConfigs.addClickHandler(new LoadGazelleConfigsClickHandler(toolkitService, myContainer, "ALL"));

		/* new code for testkit update */
        TestkitConfigTool tkconf=new TestkitConfigTool(myContainer,toolkitService);
        topPanel.add(tkconf);
	}

	class Saver implements ClickHandler {

		public void onClick(ClickEvent event) {
			props.clear();
			for (int row=0; row<grid.getRowCount(); row++) {
				String name = grid.getText(row, 0);
				Object o = grid.getWidget(row, 1);
				String value;
				if (o instanceof TextBox)
					value = ((TextBox)o).getText();
				else {
					ListBox lb = (ListBox)o;
					int i = lb.getSelectedIndex();
					value = lb.getItemText(i);
				}
//				TextBox tb = (TextBox) grid.getWidget(row, 1);
//				String value = tb.getText();
                name = name.replace(' ', '_');
				props.put(name, value);
			}
			savePropertyFile();
		}
		
	}

    /**
     * Build the grid of toolkit properties for display. The property names (keys) will be correctly formatted for
     * display here as long as underscores are used in the toolkit properties file.
     */
    void loadPropertyGrid() {
        grid.clear();
        gridRow = 0;
        for (String key : props.keySet()) {

            // create the label for each row
            String formattedKey = key.trim().replace('_', ' ');
            grid.setText(gridRow, 0, formattedKey);

            // create the boxed value for each row
            TextBox tb = new TextBox();
            tb.setWidth("600px");
            String value = props.get(key);
            tb.setText(value);
            grid.setWidget(gridRow, 1, tb);

            gridRow++;
        }
    }



    void savePropertyFile() {
		toolkitService.setToolkitProperties(props, savePropertiesCallback);
	}
	
	AsyncCallback<String> savePropertiesCallback = new AsyncCallback<String> () {

		public void onFailure(Throwable caught) {
			new PopupMessage("setToolkitProperties() call failed: " + caught.getMessage());
		}

		public void onSuccess(String result) {
			new PopupMessage("Properties saved");
			Xdstools2.getInstance().loadTkProps();  // this may now be accessible if it wasn't before
		}
		
	};
	
	void loadPropertyFile() {
		toolkitService.getToolkitProperties(getToolkitPropertiesCallback);
	}
	
	AsyncCallback<Map<String, String>> getToolkitPropertiesCallback = new AsyncCallback<Map<String, String>> () {

		public void onFailure(Throwable caught) {
			new PopupMessage("getPropertyFile() call failed: " + caught.getMessage());
		}

		public void onSuccess(Map<String, String> result) {
			props = result;
			loadPropertyGrid();
		}
		
	};
	
	
	int getIndex(ListBox lb, String value) {
		for (int i=0; i<lb.getItemCount(); i++) {
			String lbVal = lb.getItemText(i);
			if (value.equals(lbVal)) 
				return i;
		}
		return -1;
	}
	
	void addToPropertyGrid(String key, Widget w) {
		grid.setText(gridRow, 0, key);
		grid.setWidget(gridRow, 1, w);
		gridRow++;
	}


	public String getWindowShortName() {
		return "toolconfig";
	}

}
