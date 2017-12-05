package edu.cmu.inmind.multiuser.controller.session;

import edu.cmu.inmind.multiuser.controller.communication.ZMsgWrapper;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestrator;
import edu.cmu.inmind.multiuser.controller.resources.Config;

/**
 * Created by oscarr on 11/6/17.
 */
public interface Session {
    String getId();
    Config getConfig();
    String getFullAddress();
    void setId(String id, ZMsgWrapper msg, String fullAddress);
    ProcessOrchestrator getOrchestrator();
}
