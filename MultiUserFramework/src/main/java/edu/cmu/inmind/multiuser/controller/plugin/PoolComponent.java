package edu.cmu.inmind.multiuser.controller.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by oscarr on 3/10/17.
 * This annotation should be used by those stateless components that can setId multiple instances of itself (a pool of
 * instances)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PoolComponent {

}
