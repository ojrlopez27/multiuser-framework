package edu.cmu.inmind.multiuser.controller.plugin;

import java.lang.annotation.*;

/**
 * Created by oscarr on 4/18/18.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Preconditions.class)
public @interface Pre {
    String premise();
    double weight();
}
