package gov.nist.toolkit.xdstools2.client.widgets.buttons;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Image;

/**
 * Created by Diane Azais local on 11/29/2015.
 */
public class ButtonFactory {

   public static IconButton createIconButton(ButtonType _buttonType, String _tooltip){
       IconButton button;

        switch(_buttonType){
            case PLAY_BUTTON:
                button = new PlayButton(_tooltip);
                break;
            case DELETE_BUTTON:
                button = new DeleteButton(_tooltip);
                break;
            case REFRESH_BUTTON:
                button = new RefreshButton(_tooltip);
                break;
            default:
                throw new RuntimeException("Required button of unknown type.");
        }
       return button;
   }


}


