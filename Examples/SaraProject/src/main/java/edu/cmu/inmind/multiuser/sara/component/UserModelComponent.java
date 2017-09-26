package edu.cmu.inmind.multiuser.sara.component;

import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.ErrorMessages;
import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.common.model.SaraOutput;
import edu.cmu.inmind.multiuser.common.model.UserIntent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.plugin.StateType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by oscarr on 3/16/17.
 */
@StateType(state = Constants.STATEFULL )
@BlackboardSubscription( messages = {SaraCons.MSG_NLU, "MSG_START_DM"} )
public class UserModelComponent extends PluggableComponent {

    // this is out user model (though you may want to create something more sophisticated)
    private HashMap<String, List<String>> userModel;
    private final static String USER_INTEREST = "user-interests";
    private final static String PATH = "/Users/oscarr/Development/UserModel/";
    private final static String FILE_NAME = "MyUserModel.json";

    @Override
    public void startUp(){
        try{
            super.startUp();
            // TODO: add code to initialize this component
            userModel = Utils.fromJsonFile( PATH + FILE_NAME, HashMap.class);
            if( userModel == null ) userModel = new HashMap<>();
        }catch(Exception e){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.FILE_NOT_EXISTS, "User Model") );
        }

    }

    @Override
    public void execute() {
        //TODO: what should it go here? maybe store the user model in a DB? you may want to use an ORM such as Hybernate
    }

    @Override
    public void onEvent(BlackboardEvent event) {
        // Since this component is listening to NLUComponent messages, you can cast event.getElement to any kind of
        // known representation for user intents. For instance:
        UserIntent userIntent = ((SaraOutput)event.getElement()).getUserIntent();

        //now you can save user preferences on your user model
        if( userIntent.getUserIntent().equals( USER_INTEREST ) ){
            List<String> currentPreferences = userModel.get( USER_INTEREST );
            if( currentPreferences == null ) currentPreferences = new ArrayList<>();
            for( String preference : userIntent.getEntitities() ){
                if( !currentPreferences.contains(preference) ) {
                    currentPreferences.add( preference );
                }
            }
            userModel.put( USER_INTEREST, currentPreferences );
        }
    }

    @Override
    public void shutDown() {
        super.shutDown();
        // TODO: add code to release resources
        // you can store the User Model on disk (DB, File, Json, etc).
         Utils.toJsonFile( userModel, PATH, FILE_NAME);
    }
}
