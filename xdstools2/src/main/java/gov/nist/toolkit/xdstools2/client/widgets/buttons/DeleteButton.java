package gov.nist.toolkit.xdstools2.client.widgets.buttons;

import com.google.gwt.user.client.ui.Image;
import gov.nist.toolkit.xdstools2.client.resources.TestsOverviewResources;

/**
 * Created by Diane Azais local on 11/29/2015.
 */
public class DeleteButton extends IconButton {
    TestsOverviewResources RESOURCES = TestsOverviewResources.INSTANCE;
    private Image REMOVE_ICON = new Image(RESOURCES.getDeleteIconBlack());


    DeleteButton(String _tooltip){
       super(ButtonType.DELETE_BUTTON, _tooltip);
        setIcon();
    }

    @Override
    protected void setIcon() {
        getElement().appendChild(REMOVE_ICON.getElement());
    }

}
