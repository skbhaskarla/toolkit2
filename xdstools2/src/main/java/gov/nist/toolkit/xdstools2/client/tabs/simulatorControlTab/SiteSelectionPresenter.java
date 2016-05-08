package gov.nist.toolkit.xdstools2.client.tabs.simulatorControlTab;

import com.google.gwt.user.client.ui.Panel;
import gov.nist.toolkit.xdstools2.client.ToolkitServiceAsync;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class SiteSelectionPresenter {

    SelectionDisplay view;
    List<String> siteNames = new ArrayList<>();
    final static String none = "None";

    /**
     *
     * @param results
     * @param selected
     * @param panel
     */
    public SiteSelectionPresenter(List<String> results, final List<String> selected, final Panel panel) {

            siteNames.addAll(results);
//                siteNames.add(0, none);
            view = new SingleSelectionView();
            view.setData(siteNames);

            List<Integer> selectedRows = new ArrayList<>();
            for (String sel : selected) {
                if (siteNames.contains(sel))
                    selectedRows.add(siteNames.indexOf(sel));
            }
            view.setSelectedRows(selectedRows);

            bind();
            panel.add(view.asWidget());


    }

    /**
     *
     * @param toolkitService
     * @param selected
     * @param panel
     */
    public SiteSelectionPresenter(ToolkitServiceAsync toolkitService, final List<String> selected, final Panel panel) {
        // Sorry, this method is not yet implemented.
        // It can be added when all sites are desired without a transactionType filter.
    }

    void bind() {}

    public List<String> getSelected() {
        List<String> selected = new ArrayList<>();
        for (int row : view.getSelectedRows()) {
            String code = siteNames.get(row);
            if (!none.equals(code))
                selected.add(code);
        }
        return selected;
    }

    public List<String> getSiteNames() {
        return siteNames;
    }
}
