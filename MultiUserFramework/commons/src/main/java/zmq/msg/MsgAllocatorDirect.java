package zmq.msg;

import zmq.Msg;

import java.nio.ByteBuffer;

public class MsgAllocatorDirect implements MsgAllocator
{
    @Override
    public Msg allocate(int size)
    {
        return new Msg(ByteBuffer.allocateDirect(size));
    }
}
