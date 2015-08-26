package gov.nist.toolkit.xdstools2.client;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import gov.nist.toolkit.results.client.CodesConfiguration;

/**
 * Created by bill on 8/25/15.
 */
public class CodeFilter {
    public String codeName;
    public ListBox selectedCodes = new ListBox();
    public Button editButton = new Button("edit");

    HTML label = new HTML();

    public CodeFilter(FlexTable mainGrid, int row, int startingCol, String codeName) {
        mainGrid.setWidget(row, startingCol, label);

        selectedCodes.setVisibleItemCount(1);
        mainGrid.setWidget(row, startingCol+1, selectedCodes);

        mainGrid.setWidget(row, startingCol+2, editButton);
        editButton.setEnabled(false);

        setLabelText(CodesConfiguration.titles.get(codeName));
        setCodeName(codeName);
    }

    public CodeFilter(FlexTable mainGrid, int row, int startingCol, String labelText, String codeName, int codeBoxSize) {
        this(mainGrid, row, startingCol, codeName);
        setLabelText(labelText);
        setCodeBoxSize(codeBoxSize);
    }

    public void setCodeBoxSize(int size) { selectedCodes.setVisibleItemCount(size); }
    public void setLabelText(String text) { label.setText(text); }
    public void setCodeName(String name) { codeName = name; }
}
