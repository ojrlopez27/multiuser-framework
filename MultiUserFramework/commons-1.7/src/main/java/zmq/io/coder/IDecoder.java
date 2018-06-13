package zmq.io.coder;

import zmq.Msg;
import zmq.util.ValueReference;

import java.nio.ByteBuffer;

public interface IDecoder
{
    interface Step
    {
        enum Result
        {
            MORE_DATA(0),
            DECODED(1),
            ERROR(-1);

            @SuppressWarnings("unused")
            // reminder for c++ equivalent
            private final int code;

            Result(int code)
            {
                this.code = code;
            }
        }

        Result apply();
    }

    ByteBuffer getBuffer();

    Step.Result decode(ByteBuffer buffer, int size, ValueReference<Integer> processed);

    Msg msg();

    void destroy();
}
