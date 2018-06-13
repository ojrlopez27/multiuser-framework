package edu.cmu.inmind.multiuser.controller.common;


import com.rits.cloning.Cloner;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oscarr on 4/20/16.
 */
public class Utils {

    /**********************************************************************************************/
    /************************************** CLONE *************************************************/
    /**********************************************************************************************/

    private static Cloner cloner = new Cloner();

    public static <T> T clone( T object ){
        return cloner.shallowClone(object);
    }

    public static <T> T deepClone( T object ){
        return cloner.deepClone(object);
    }

    public static <T extends List> T cloneList(T list ){
        return cloner.deepClone(list);
    }

    public static ArrayList cloneArray( ArrayList list ){
        ArrayList result = new ArrayList(list.size());
        for( Object element : list ){
            result.add( cloner.deepClone(element) );
        }
        return result;
    }

}
