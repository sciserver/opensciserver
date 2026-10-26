package compm.application.rdb.stream;

import java.io.OutputStream;


/**
 * Represents a channel or way for sending an output as a streamed content.
 */
public abstract class StreamChannel {

    protected OutputStream outputStream;

    public StreamChannel() {

    }

    /**
     * Abstract method for opening this output stream.
     */
    public abstract void open() throws Exception;

    /**
     * Abstract method for closing this output stream.
     */
    public abstract void close() throws Exception;

    /**
     * Abstract method for getting this output stream.
     */
    public OutputStream getOutputStream() {
        return this.outputStream;
    }
}
