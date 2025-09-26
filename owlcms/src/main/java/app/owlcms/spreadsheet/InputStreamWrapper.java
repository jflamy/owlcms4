package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lightweight InputStream wrapper that rethrows an IOException recorded by the writer task.
 */
public class InputStreamWrapper extends InputStream {
    private final InputStream delegate;
    private final AtomicReference<IOException> writerException;

    public InputStreamWrapper(InputStream delegate, AtomicReference<IOException> writerException) {
        this.delegate = delegate;
        this.writerException = writerException;
    }

    /**
     * Return any IOException recorded by the writer task, or null if none yet.
     */
    public IOException getWriterException() {
        return writerException.get();
    }

    private void rethrowWriterIfFailed() throws IOException {
        IOException w = writerException.get();
        if (w != null) throw w;
    }

    @Override
    public int read() throws IOException {
        rethrowWriterIfFailed();
        int r = delegate.read();
        rethrowWriterIfFailed();
        return r;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        rethrowWriterIfFailed();
        int r = delegate.read(b, off, len);
        rethrowWriterIfFailed();
        return r;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int available() throws IOException {
        rethrowWriterIfFailed();
        return delegate.available();
    }

    @Override
    public void close() throws IOException {
        try {
            rethrowWriterIfFailed();
        } finally {
            delegate.close();
        }
    }
}
