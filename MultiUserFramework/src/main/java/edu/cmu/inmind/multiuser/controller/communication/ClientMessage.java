package edu.cmu.inmind.multiuser.controller.communication;

import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.Pair;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by oscarr on 6/28/17.
 */
public class ClientMessage {
    private final LinkedList<Pair<String, Object>> messageQueue = new LinkedList<>();
    // lock and condition variables
    private Lock aLock = new ReentrantLock();
    private Condition bufferNotFull = aLock.newCondition();
    private Condition bufferNotEmpty = aLock.newCondition();
    private long timeToWait = 15;

    public void put(Pair<String, Object> message){
        //Log4J.debug(this, "putting: " + message.toString());
        boolean isLocked = false;
        try {
            isLocked = aLock.tryLock( timeToWait * 10, TimeUnit.MILLISECONDS);
            if( isLocked ) {
                while (messageQueue.size() >= Constants.QUEUE_CAPACITY) {
                    bufferNotEmpty.await(timeToWait * 10, TimeUnit.MILLISECONDS);
                    //we start to lose messages :(
                    messageQueue.clear();
                }

                boolean isAdded = messageQueue.offer(message);
                if (isAdded) {
                    bufferNotFull.signalAll();
                }
            }
        }catch (Throwable e) {
            ExceptionHandler.handle(e);
        }finally {
            if( isLocked ) aLock.unlock();
        }
        //Log4J.debug(this, "done putting ...");
    }

    public Pair<String, Object> get(){
        //Log4J.debug(this, "attempting to get ...");
        boolean isLocked = false;
        Pair<String, Object> value = null;
        try {
            isLocked = aLock.tryLock(timeToWait * 10, TimeUnit.MILLISECONDS );
            if( isLocked ) {
                while (messageQueue.size() == 0) {
                    bufferNotFull.await();
                }
                value = messageQueue.poll();
                if (value != null) {
                    bufferNotEmpty.signalAll();
                }
            }
        } catch(Throwable e){
            ExceptionHandler.handle( e );
        } finally{
            if( isLocked ) aLock.unlock();
        }
        //Log4J.debug(this, "got: " + value.toString());
        return value;
    }

    //TODO: who needs to call thi?
    public void reset() {
        aLock = new ReentrantLock();
        bufferNotFull = aLock.newCondition();
        bufferNotEmpty = aLock.newCondition();
        messageQueue.clear();
    }
}
