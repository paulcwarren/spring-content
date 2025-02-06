package internal.org.springframework.content.encryption.engine;

import java.io.IOException;
import java.io.InputStream;

/**
 * Adds a fixed amount of 0-bytes in front of the delegate {@link InputStream}
 */
class ZeroPrefixedInputStream extends InputStream {
    private final InputStream delegate;
    private long prefixBytes;

    public ZeroPrefixedInputStream(InputStream delegate, long prefixBytes) {
        this.delegate = delegate;
        this.prefixBytes = prefixBytes;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        if(n <= prefixBytes) {
            prefixBytes -= n;
            return n;
        }
        if(prefixBytes > 0) {
            n = n - prefixBytes; // Still skipping so many bytes from the offset
            try {
                return prefixBytes + delegate.skip(n);
            } finally {
                prefixBytes = 0; // Now the whole offset is consumed; skip to the delegate
            }
        }

        return delegate.skip(n);
    }

    @Override
    public int read() throws IOException {
        if(prefixBytes > 0) {
            prefixBytes--;
            return 0;
        }
        return delegate.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if(prefixBytes > 0) {
            return super.read(b, off, len);
        }
        return delegate.read(b, off, len);
    }

    @Override
    public int available() throws IOException {
        if(prefixBytes > 0) {
            return (int)Math.max(prefixBytes, Integer.MAX_VALUE);
        }
        return delegate.available();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
