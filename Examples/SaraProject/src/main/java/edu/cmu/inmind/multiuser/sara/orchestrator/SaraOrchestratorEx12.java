package edu.cmu.inmind.multiuser.sara.orchestrator;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestratorImpl;
import edu.cmu.inmind.multiuser.controller.session.Session;
import edu.cmu.inmind.multiuser.sara.component.HowToLogComponent;

/**
 * Created by oscarr on 3/3/17.
 */
@BlackboardSubscription( messages = {SaraCons.MSG_NLU})
public class SaraOrchestratorEx12 extends ProcessOrchestratorImpl {

    @Override
    public void initialize(Session session) throws Throwable{
        // TODO: If you want to use your own implementation of a messageLogger, you have to declare it here in the
        // TODO: orchestrator's constructor. For instance, do something like this:
        // messageLogger = new SaraDBLogger();
        // messageLogger.setPath( "your-db-connection-string-goes-here");

        super.initialize( session );
    }

    @Override
    public void process(String message) {
        super.process(message);

        // All messages that goes through the Blackboard are "automatically" posted to a MessageLogger (a class that
        // implements MessageLog interface). By default, the MessageLogger is implemented by FileLogger, which stores
        // all messages into a file given a specific path (in the Config object). However, if you need a different kind
        // of logger (e.g., a DB) you must implement MessageLogger interface and add it to your orchestrator (see method
        // initialize at SARAProcessOrchestrator).

        // by default, the messageLogger records all the changes on blackboard. If you want to turn this off, do this:
        getBlackboard().setLoggerOn( true );
        blackboard.post(this, "LoggerTest", "This should be stored in the log");
        getBlackboard().setLoggerOn( false );
        blackboard.post(this, "LoggerTest", "This should not be stored in the log");

        // Also, you can explicitly post messages to MessageLogger like this (look at execute() method implementation at
        // HowToLogComponent):
        HowToLogComponent component = get( HowToLogComponent.class );
        execute( component );

        // Or you can use Loggable annotation to automatically generate an entry in the logger with information about the
        // called method and its arguments (in this case, HowToLogComponent.anotherMethod is annotated with Loggable):
        component.anotherMethod("this-is-a-test ");

        // by default, the log is stored on disk when you finish the session (e.g., you type shutdown) but you can force
        // it like this:
        try {
            logger.store();
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }

        // MessageLogger is different to Log4J, which only prints messages out on console (unless you specify something
        // different in the appenders section at log4j2.xml file)
        Log4J.info(this, "You should see this in your console");

        //You can turn off all the output logs for Log4J like this:
        Log4J.turnOn(false);
        Log4J.info(this, "You should NOT see this in your console");
    }


    @Override
    public void start() {
        super.start();
        //TODO: add some logic when session is started (e.g., startUp resources)
    }

    @Override
    public void pause() {
        super.pause();
        //TODO: add some logic when session is paused (e.g., stop temporarily execute execution)
    }

    @Override
    public void resume() {
        super.resume();
        //TODO: add some logic when session is resumed (e.g., resume execute execution)
    }

    @Override
    public void close() throws Throwable{
        super.close();
        //TODO: add some logic when session is closed (e.g., release resources)
    }
}
