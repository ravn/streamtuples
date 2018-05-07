package dk.kb.stream;

/**
 * Inspired by http://markhneedham.com/blog/2014/02/26/java-8-lambda-expressions-vs-auto-closeable/
 */
public class WasClosed implements AutoCloseable {
    private boolean closed = false;

    @Override
    public void close() throws Exception {
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }
}
