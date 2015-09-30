package gov.nist.toolkit.xdstools2.client.widgets.queryFilter;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import gov.nist.toolkit.xdstools2.client.AuthorPicker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Diane Azais local on 9/30/2015.
 */
public class AuthorFilter extends Widget implements QueryFilter {
    public String codeName;
    public ListBox inputAuthorList = new ListBox();
    public Button editButton = new Button("edit");
    HTML label = new HTML();


    // label is required non-empty but is never displayed
    public AuthorFilter(FlexTable mainGrid, int row, int startingCol, String labelText, String codeName) {
        mainGrid.setWidget(row, startingCol, label);

        inputAuthorList.setVisibleItemCount(1);
        mainGrid.setWidget(row, startingCol + 1, inputAuthorList);

        mainGrid.setWidget(row, startingCol + 2, editButton);
        editButton.setEnabled(true);
        editButton.addClickHandler(
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        try {
                            new AuthorPicker(inputAuthorList).show();
                        } catch (Exception e) {
                            //genericQueryTab.setStatus(e.getMessage(), false);
                        }
                    }
                });

        setLabelText(labelText);
        setCodeName(codeName);
        setAuthorBoxSize(2); //TODO this should be combined with same settings in CodeFilterBank via a superclass
    }



   // public Widget asWidget() { return hp; }

    @Override
    /**
     * Adds the values selected by the user to a list of accepted values for the document search
     */
    public void addToCodeSpec(Map<String, List<String>> codeSpec, String codeType) {
        codeSpec.put(codeType, getValues());
    }

    List<String> getValues() {
        List<String> values = new ArrayList<>();
        //TODO fill out values
        return values;
    }

    public void setAuthorBoxSize(int size) { inputAuthorList.setVisibleItemCount(size); }
    public void setLabelText(String text) { label.setText(text); }
    public void setCodeName(String name) { codeName = name; }
}
