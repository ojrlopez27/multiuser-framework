package edu.cmu.inmind.multiuser.controller.plugin;

import java.lang.reflect.Method;

/**
 * Created by oscarr on 4/18/18.
 */
public class TestPrecondition {

    @Preconditions({
            @Pre(premise = Const.LOCATION_CHANGED, weight = 0.5),
            @Pre(premise = Const.BATTERY_ENERGY_NOT_LOW, weight = 0.3)})
    public void methodOne(){
        System.out.println("do something");
    }

    public static void main(String args[]) throws Exception{
        TestPrecondition testPrecondition = new TestPrecondition();
        Method method = testPrecondition.getClass().getDeclaredMethod("methodOne");
        if( method.isAnnotationPresent(Preconditions.class) ){
            for(Pre precondition : method.getAnnotation(Preconditions.class).value()){
                System.out.println("precondition: " + precondition.premise() + ", " + precondition.weight());
            }
        }
    }
}
