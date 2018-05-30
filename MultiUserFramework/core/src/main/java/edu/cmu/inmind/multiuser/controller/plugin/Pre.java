package edu.cmu.inmind.multiuser.controller.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by oscarr on 4/18/18.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
//@Repeatable(Preconditions.class)
public @interface Pre {
    String premise();
    double weight();
}
