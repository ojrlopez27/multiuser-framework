package edu.cmu.inmind.multiuser.controller.communication;

import org.zeromq.ZFrame;
import org.zeromq.ZMsg;

/**
 * Created by oscarr on 3/31/17.
 */
public class ZMsgWrapper {
    private ZMsg msg;
    private ZFrame replyTo;

    public ZMsgWrapper() {
    }

    public ZMsgWrapper(ZMsg msg, ZFrame replyTo) {
        this.msg = msg;
        this.replyTo = replyTo;
    }

    public ZMsg getMsg() {
        return msg;
    }

    public ZFrame getReplyTo() {
        return replyTo;
    }

    public ZMsgWrapper duplicate(){
        if( msg == null || replyTo == null ){
            return this;
        }
        return new ZMsgWrapper( this.getMsg().duplicate(), this.getReplyTo().duplicate() );
    }

    public void destroy(){
        if(msg != null) msg.destroy();
        if(replyTo != null) replyTo.destroy();
    }
}
