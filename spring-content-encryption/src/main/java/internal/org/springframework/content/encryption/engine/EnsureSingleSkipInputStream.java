package internal.org.springframework.content.encryption.engine;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Ensures that a single {@link #skip(long)} call skips exactly that amount of bytes.
 * <p>
 * This fixes an issue in the {@link javax.crypto.CipherInputStream} where skips can stop short of the requested skip amount
 */
class EnsureSingleSkipInputStream extends FilterInputStream {

    public EnsureSingleSkipInputStream(InputStream in) {
        super(in);
    }

    @Override
    public long skip(long n) throws IOException {
        long totalSkipped = 0;
        while(totalSkipped < n) {
            var skipAmount = super.skip(n-totalSkipped);
            totalSkipped+=skipAmount;
            if(skipAmount == 0) { // no bytes were skipped
                // Read one byte to check for EOF
                if(read() == -1) {
                    return totalSkipped;
                }
                totalSkipped++; // We skipped the byte we read above
            }
        }
        return n;
    }
}
