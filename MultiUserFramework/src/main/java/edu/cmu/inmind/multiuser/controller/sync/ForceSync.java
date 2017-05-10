package edu.cmu.inmind.multiuser.controller.sync;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by oscarr on 3/11/17.
 * Use this annotation to identify asynchronous components that will be forced to be executed synchronously
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ForceSync {
    String id() default "force-sync";
}

