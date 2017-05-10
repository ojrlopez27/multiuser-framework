package edu.cmu.inmind.multiuser.controller.communication;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by oscarr on 3/14/17.
 * This annotation allows a PluginComponent to communicate with a remote service. "remoteService" must match the same
 * id as the remote service is registered in the system.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@BindingAnnotation
public @interface ConnectRemoteService {
    String remoteService();
}
