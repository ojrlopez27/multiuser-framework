package edu.cmu.inmind.multiuser.controller.log;

import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestrator;
import edu.cmu.inmind.multiuser.controller.plugin.Pluggable;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Created by oscarr on 3/22/17.
 */
public class LoggerInterceptor implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Object thisObj = methodInvocation.getThis();
        if( thisObj instanceof ProcessOrchestrator){
            ((ProcessOrchestrator) thisObj).getLogger().add( methodInvocation.getMethod().getName(),
                    argsToString(methodInvocation) );
        }else if( thisObj instanceof Pluggable){
            MessageLog log = ((Pluggable) thisObj).getMessageLogger();
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