package internal.org.springframework.content.s3.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.util.Assert;

/**
 * A partial content input stream wrapper around a delegate {@link InputStream}.
 * <p>
 * The delegate is expected to be a "prepared" input stream for a certain byte-range of the original resource.
 * Only bytes inside the byte-range of the original resource can be read successfully,
 * all bytes outside the available byte-range will return zero-bytes.
 */
class PartialContentInputStream extends InputStream {

    private final InputStream delegate;
    private final long totalLength;
    private final long rangeStart;
    private final long rangeEnd;
    private long currentPosition = 0;

    // As per https://www.rfc-editor.org/rfc/rfc9110.html#field.content-range
    private static final Pattern CONTENT_RANGE_PATTERN = Pattern.compile("\\Abytes (?<firstPos>[0-9]+)-(?<lastPos>[0-9]+)/(?<completeLength>[0-9]+|\\*)\\Z");

    public static InputStream fromContentRange(InputStream delegate, String contentRange) {
        Assert.notNull(delegate, "delegate");
        Assert.notNull(contentRange, "contentRange");
        var contentRangeMatch = CONTENT_RANGE_PATTERN.matcher(contentRange);
        if(!contentRangeMatch.matches()) {
            throw new IllegalArgumentException("Content-Range '%s' is not RFC9110 compliant.".formatted(contentRange));
        }
        var rangeStart = Long.parseUnsignedLong(contentRangeMatch.group("firstPos"));
        // HTTP Content-Range has last byte inclusive; but this object requires an exclusive boundary
        var rangeEnd = Long.parseUnsignedLong(contentRangeMatch.group("lastPos")) + 1;
        var completeLengthStr = contentRangeMatch.group("completeLength");
        var completeLength = Objects.equals(completeLengthStr, "*")?rangeEnd:Long.parseUnsignedLong(completeLengthStr);
        return new PartialContentInputStream(delegate, completeLength, rangeStart, rangeEnd);
    }

    public PartialContentInputStream(
            InputStream delegate,
            long totalLength,
            long rangeStart,
            long rangeEndExclusive
    ) {
        this.delegate = delegate;
        if(totalLength < rangeEndExclusive) {
            // Total length can not be less than the end of the range
            // Note that this also covers the case where the total length is unknown
            totalLength = rangeEndExclusive;
        }
        this.totalLength = totalLength;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEndExclusive;
    }

    private BlockState blockState(long length) {
        if(currentPosition + length < 0) { // Sum is negative -> overflow of currentPosition. Reduce n to a more sensible value.
            length = Long.MAX_VALUE - currentPosition; // maximum value of length (will be MAX_VALUE after adding currentPosition + length)
        }
        var startPosition = currentPosition;
        var endPosition = Math.min(currentPosition + length, totalLength); // We can skip at most to the total length
        if (endPosition < rangeStart || startPosition >= rangeEnd) {
            // Our end position is before the start of the range that we're holding
            // Or our start position is after the end of the range that we're holding
            // All positions between are outside the range
            return new BlockState(
                    false,
                    startPosition,
                    endPosition
            );
        } else if (startPosition < rangeStart /* implicit by condition above: && endPosition >= rangeStart */) {
            // We are starting outside the range. Go right up until the start of the range only
            return new BlockState(
                    false,
                    startPosition,
                    rangeStart
            );
        } else {
            // We are currently inside the range. Go right up until the end of the range at most
            return new BlockState(
                    true,
                    startPosition,
                    Math.min(endPosition, rangeEnd)
            );
        }
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) {
            // Negative number requested; don't skip at all
            return 0;
        }

        var startPosition = this.currentPosition;
        var positionState = blockState(n);

        if(positionState.insideRange()) {
            // Skip on the delegate stream (however much it wants to skip) and update our current position
            // We know that the delegate stream can not skip beyond EOF, so no end position check is necessary
            this.currentPosition += delegate.skip(n);
        } else {
            this.currentPosition = positionState.endPosition();
        }

        return currentPosition - startPosition;
    }

    @Override
    public int read()
            throws IOException {
        if(currentPosition >= totalLength) {
            return -1;
        }
        var positionState = blockState(1);
        try {
            if(positionState.insideRange()) {
                // Current position is inside the range, read from the delegate stream
                return delegate.read();
            }
            // We are outside the range, return a NUL byte for all data outside the available range
            return 0;
        } finally {
            currentPosition++;
        }
    }

    @Override
    public int read(byte[] buffer, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, buffer.length);
        if(currentPosition >= totalLength) {
            return -1;
        }
        var positionState = blockState(len);
        if(positionState.insideRange()) {
            var readBytes = delegate.read(buffer, off, positionState.intLength());
            this.currentPosition += readBytes;
            return readBytes;
        } else {
            // Outside of the range; fill the necessary part of the buffer with NUL bytes
            Arrays.fill(buffer, off, off+positionState.intLength(), (byte)0);
            this.currentPosition = positionState.endPosition();
            return positionState.intLength();
        }
    }

    @Override
    public String toString() {
        return "PartialContentInputStream[range=%d-%d; delegate=%s]".formatted(rangeStart, rangeEnd, delegate);
    }

    @Override
    public int available() throws IOException {
        var state = blockState(Integer.MAX_VALUE);
        if(state.insideRange()) {
            return delegate.available();
        } else {
            return state.intLength();
        }
    }

    @Override
    public void close()
            throws IOException {
        delegate.close();
    }

    private record BlockState(
            boolean insideRange,
            long startPosition,
            long endPosition
    ) {

        public long length() {
            return endPosition - startPosition;
        }

        public int intLength() {
            return (int)Math.min(length(), Integer.MAX_VALUE);
        }

    }
}
