package edu.cmu.inmind.multiuser.controller;

import edu.cmu.inmind.multiuser.common.ErrorMessages;
import edu.cmu.inmind.multiuser.controller.communication.ServiceInfo;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.controller.resources.Config;
import edu.cmu.inmind.multiuser.controller.session.SessionManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by oscarr on 6/27/17.
 */
public class MultiuserFrameworkFactory {
    private static Map<String, MultiuserFramework> mufs = new HashMap<>();

    /**
     * This method creates an instance of the SessionManager class, which listens to new connection/
     * disconnection requests.
     * @param modules array of pluggable modules
     * @param config  settings for the MUF
     * @param serviceInfo your MUF can be either master or slave. The master MUF receives messages directly
     *                    from clients, but slave MUF receives messages from the master MUF. If your MUF
     *                    is a slave, then provide connection information about it.
     */
    public static MultiuserFramework startFramework(PluginModule[] modules, Config config, ServiceInfo serviceInfo )
            throws MultiuserException{
        String id = config.getServerAddress() + ":" + config.getSessionManagerPort();
        if( null != mufs.get( id ) ){
            throw new MultiuserException(ErrorMessages.FRAMEWORK_ALREADY_EXIST, id );
        }
        MultiuserFramework muf = new MultiuserFramework( new SessionManager( modules, config, serviceInfo ), id );
        mufs.put( id, muf );
        muf.start();
        return muf;
    }

    /**
     * If your MUF is a master one, you don't need to provide information about the service.
     * @param modules
     * @param config
     */
    public static void startFramework( PluginModule[] modules, Config config ) throws MultiuserException{
        startFramework( modules, config, null);
    }

    public static void stopFramework( MultiuserFramework muf ){
        muf.stop();
    }
}
