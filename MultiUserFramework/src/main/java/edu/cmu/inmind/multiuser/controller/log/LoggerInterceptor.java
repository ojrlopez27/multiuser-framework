package edu.cmu.inmind.multiuser.controller.log;

import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestratorImpl;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Created by oscarr on 3/22/17.
 */
public class LoggerInterceptor implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Object thisObj = methodInvocation.getThis();
        if( thisObj instanceof ProcessOrchestratorImpl ){
            ((ProcessOrchestratorImpl) thisObj).getLogger().add( methodInvocation.getMethod().getName(),
                    argsToString(methodInvocation) );
        }else if( thisObj instanceof PluggableComponent){
            MessageLog log = ((PluggableComponent) thisObj).getMessageLogger();
            if( log != null ) {
                log.add(methodInvocation.getMethod().getName(),
                        argsToString(methodInvocation));
            }
        }
        return methodInvocation.proceed();
    }

    private String argsToString( MethodInvocation methodInvocation ) throws Throwable{
        Object[] args = methodInvocation.getArguments();
        if( args != null && args.length > 0 ){
            StringBuffer stringBuffer = new StringBuffer( "[" );
            for(Object obj : args ){
                stringBuffer.append( obj.toString() + ", ");
            }
            return stringBuffer.replace( stringBuffer.lastIndexOf(", "), stringBuffer.length(), "]" ).toString();
        }
        return "";
    }
}