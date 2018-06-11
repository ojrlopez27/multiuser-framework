package edu.cmu.inmind.multiuser.controller.blackboard;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by oscarr on 3/15/17.
 * Add this annotation to indicate to which messages the PluggableComponent is subscribed to.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BlackboardSubscription {
    String[] messages();
}
