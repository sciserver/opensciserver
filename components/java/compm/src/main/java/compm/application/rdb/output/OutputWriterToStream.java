package compm.application.rdb.output;

import compm.application.rdb.stream.StreamChannel;
import java.io.IOException;
import java.io.OutputStream;


/**
 * Provides methods for writing to an output though a stream channel.
 */
public abstract class OutputWriterToStream extends OutputWriter {

    StreamChannel streamChannel;

    public OutputWriterToStream(StreamChannel streamChannel, Long numRowsPerFlush) {
        super(numRowsPerFlush);
        this.streamChannel = streamChannel;
    }

    /**
     * Opens the stream channel and the writer/printer.
     */
    @Override
    public final void open() throws Exception {
        streamChannel.open();
        open_();
    }

    /**
     * Abstract method for specifically opening the writer or printer on the stream.
     */
    public abstract void open_() throws Exception;

    /**
     * Flushes the writer or printer on the stream, and then flushes the stream
     * channel.
     */
    @Override
    public final void flush() throws Exception {
        flush_();
        getOutputStream().flush();
    }

    /**
     * Abstract method for specifically flusing the writer or printer.
     */
    public abstract void flush_() throws Exception;

    /**
     * used for closing, by flushing everything and closing the the writer or
     * printer on the stram and then closing the stream channel.
     */
    @Override
    public final void close() throws Exception {
        flush();
        close_();
        streamChannel.close();
    }

    /**
     * Abstract method for specifically closing the writer or printer on the stream.
     */
    public abstract void close_() throws Exception;

    /**
     * in case an exception occurs, cleans up the writer or printer on the stream,
     * flushes the stream, and closes the stream and stream channel.
     */
    @Override
    public final void cleanUp() {
        cleanUp_();
        try {
            getOutputStream().flush();
        } catch (Exception e) {
        }
        ;
        try {
            getOutputStream().close();
        } catch (Exception e) {
        }
        ;
        try {
            streamChannel.close();
        } catch (Exception e) {
        }
        ;
    }

    /**
     * Abstract method for specifically cleaning up the writer or printer on the
     * stream, in case an exception occurs.
     */
    public abstract void cleanUp_();

    /**
     * Gets the output stream from the stream channel.
     *
     * @return the output stream object.
     */
    public OutputStream getOutputStream() throws IOException {
        return streamChannel.getOutputStream();
    }

}
