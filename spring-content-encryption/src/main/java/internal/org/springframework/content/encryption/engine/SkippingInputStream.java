package internal.org.springframework.content.encryption.engine;

import java.io.IOException;
import java.io.InputStream;

/**
 * Skips a certain amount of bytes from the delegate {@link InputStream}
 */
class SkippingInputStream extends InputStream {
    private final InputStream delegate;
    private final long skipBytes;
    private boolean hasSkipped;

    public SkippingInputStream(InputStream delegate, long skipBytes) {
        this.delegate = delegate;
        this.skipBytes = skipBytes;
    }

    private void ensureSkipped() throws IOException {
        if(!hasSkipped) {
            delegate.skipNBytes(skipBytes);
            hasSkipped = true;
        }
    }

    @Override
    public long skip(long n) throws IOException {
        ensureSkipped();
        return delegate.skip(n);
    }

    @Override
    public int read() throws IOException {
        ensureSkipped();
        return delegate.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        ensureSkipped();
        return delegate.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
