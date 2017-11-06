package edu.cmu.inmind.multiuser.controller.muf;

import edu.cmu.inmind.multiuser.controller.common.ErrorMessages;
import edu.cmu.inmind.multiuser.controller.communication.ServiceInfo;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.controller.resources.Config;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by oscarr on 6/27/17.
 * Implementation of design pattern Object Lifetime Manager:
 * {@Link https://adtmag.com/articles/2000/02/29/object-lifetime-manager-a-complementary-pattern-for-controlling-object-creation-and-destruction.aspx}
 * {@Link https://www.researchgate.net/publication/2420486_Object_Lifetime_Manager_-_A_Complementary_Pattern_for_Controlling_Object_Creation_and_Destruction}
 */
public class MUFLifetimeManager {
    private static ConcurrentHashMap<String, MultiuserController> mufs = new ConcurrentHashMap<>();

    /**
     * This method creates an instance of the SessionManager class, which listens to new connection/
     * disconnection requests.
     * @param modules array of pluggable modules
     * @param config  settings for the MUF
     * @param serviceInfo your MUF can be either master or slave. The master MUF receives messages directly
     *                    from clients, but slave MUF receives messages from the master MUF. If your MUF
     *                    is a slave, then provide connection information about it.
     */
    public static MultiuserController startFramework(PluginModule[] modules, Config config, ServiceInfo serviceInfo )
            throws Throwable{
        String id = config.getServerAddress() + config.getSessionManagerPort();
        if( null != mufs.get( id ) ){
            throw new MultiuserException(ErrorMessages.FRAMEWORK_ALREADY_EXIST, id );
        }
        MultiuserController muf = new MultiuserController( id, modules, config, serviceInfo );
        mufs.put( id, muf );
        muf.start();
        return muf;
    }

    /**
     * If your MUF is a master one, you don't need to provide information about the service.
     * @param modules
     * @param config
     */
    public static MultiuserController startFramework(PluginModule[] modules, Config config ) throws Throwable{
       return startFramework( modules, config, null);
    }

    public static void stopFramework( MultiuserController muf ){
        mufs.remove( muf.getId() );
        muf.stop();
    }

    public static void stopFrameworks() throws Throwable{
        for (String key : mufs.keySet()) {
            mufs.remove(key).stop();
        }
    }

    public static Object get(String id) {
        return mufs.get( id );
    }
}
