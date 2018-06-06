package edu.cmu.inmind.multiuser.controller.plugin;

import com.google.inject.Provider;
import edu.cmu.inmind.multiuser.controller.common.CommonUtils;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by oscarr on 3/10/17.
 */
public class ComponentPoolProvider implements Provider<PluggableComponent> {
    private CopyOnWriteArrayList<PluggableComponent> pool;
    private Integer size;
    private Integer position;
    private Class poolType;

    public ComponentPoolProvider(Class poolType, Integer maxSize){
        this.poolType = poolType;
        this.size = maxSize;
        this.position = -1;
        this.pool = new CopyOnWriteArrayList<>();
    }

    @Override
    public PluggableComponent get() {
        if( pool.size() == size ){
            position = position == (size - 1)? 0 : ++position;
            return pool.get( position);
        }
        PluggableComponent component = (PluggableComponent) CommonUtils.createInstance( poolType );
        pool.add( component );
        return component;
    }
}
