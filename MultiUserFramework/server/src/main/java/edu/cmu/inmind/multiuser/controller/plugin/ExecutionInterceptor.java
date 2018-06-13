package edu.cmu.inmind.multiuser.controller.plugin;

import edu.cmu.inmind.multiuser.controller.composer.ProcessOrchestrator;
import edu.cmu.inmind.multiuser.controller.composer.ProcessOrchestratorImpl;
import edu.cmu.inmind.multiuser.controller.session.Session;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Created by oscarr on 3/21/17.
 */
public class ExecutionInterceptor implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        if( methodInvocation.getThis() instanceof ProcessOrchestratorImpl){
            Session session = ((ProcessOrchestrator) methodInvocation.getThis() ).getSession();
            ((PluggableComponent) methodInvocation.getArguments()[0]).setActiveSession( session );
        }
        return methodInvocation.proceed();
    }
}
