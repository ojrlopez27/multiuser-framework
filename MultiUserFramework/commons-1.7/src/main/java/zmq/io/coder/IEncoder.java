package zmq.io.coder;

import zmq.Msg;
import zmq.util.ValueReference;

import java.nio.ByteBuffer;

public interface IEncoder
{
    //  Load a new message into encoder.
    void loadMsg(Msg msg);

    //  The function returns a batch of binary data. The data
    //  are filled to a supplied buffer. If no buffer is supplied (data_
    //  points to NULL) decoder object will provide buffer of its own.
    int encode(ValueReference<ByteBuffer> data, int size);

    void destroy();
}
