package internal.org.springframework.content.s3.io;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(Ginkgo4jSpringRunner.class)
public class PartialContentInputStreamTest {

    private static final byte[] FULL_DATA = "This is a test string".getBytes(StandardCharsets.UTF_8);
    private static final byte NUL = 0;

    private InputStream inputStream;


    {
        Describe("PartialContentInputStream", () -> {
            Context("with a range from the start", () -> {
                BeforeEach(() -> {
                    inputStream = PartialContentInputStream.fromContentRange(
                            new ByteArrayInputStream(FULL_DATA, 0, 4),
                            "bytes 0-3/"+FULL_DATA.length // bytes in the range description are *inclusive*
                    );
                });
                AfterEach(() -> {
                    inputStream.close();
                });
                It("reads fully from start to finish", () -> {
                    var readData = new byte[FULL_DATA.length];
                    Arrays.fill(readData, (byte)0xba); // Fill array to detect that it is properly filled with NUL bytes by the read function
                    IOUtils.readFully(inputStream, readData);

                    assertThat(readData[0], is(equalTo(FULL_DATA[0])));
                    assertThat(readData[1], is(equalTo(FULL_DATA[1])));
                    assertThat(readData[2], is(equalTo(FULL_DATA[2])));
                    assertThat(readData[3], is(equalTo(FULL_DATA[3])));
                    for(int i = 4; i < FULL_DATA.length; i++) {
                        assertThat(readData[i], is(equalTo(NUL)));
                    }

                    // Stream is at EOF after reading all the bytes
                    assertThat(inputStream.read(), is(equalTo(-1)));
                });

                It("skips bytes into the range", () -> {
                    inputStream.skipNBytes(2);

                    var readData = new byte[6];
                    Arrays.fill(readData, (byte)0xba); // Fill array to detect that it is properly filled with NUL bytes by the read function
                    IOUtils.readFully(inputStream, readData);
                    assertThat(readData[0], is(equalTo(FULL_DATA[2])));
                    assertThat(readData[1], is(equalTo(FULL_DATA[3])));
                    assertThat(readData[2], is(equalTo(NUL)));

                });

                It("skips bytes inside the range", () -> {
                    assertThat(inputStream.read(), is(equalTo(FULL_DATA[0] & 0xff)));
                    inputStream.skipNBytes(2); // Bytes 1 & 2 are skipped
                    assertThat(inputStream.read(), is(equalTo(FULL_DATA[3] & 0xff)));
                    assertThat(inputStream.read(), is(equalTo(NUL & 0xff)));
                });

                It("skips bytes out of the range", () -> {
                    assertThat(inputStream.read(), is(equalTo(FULL_DATA[0] & 0xff)));
                    inputStream.skipNBytes(FULL_DATA.length - 1); // Skip until past the end of the range; right up until the end of the data

                    assertThat(inputStream.read(), is(equalTo(-1))); // EOF
                });

                It("skips bytes after the end of the range", () -> {
                    inputStream.skipNBytes(4); // Skip right up to the end of the range

                    assertThat(inputStream.skip(Long.MAX_VALUE), is(equalTo((long)FULL_DATA.length - 4))); // All the rest of the bytes can be skipped at once
                });

            });
            Context("with a range to the end", () -> {
                BeforeEach(() -> {
                    inputStream = PartialContentInputStream.fromContentRange(
                            new ByteArrayInputStream(FULL_DATA, 10, FULL_DATA.length-10),
                            "bytes 10-"+FULL_DATA.length+"/*"
                    );
                });
                AfterEach(() -> {
                    inputStream.close();
                });

                It("reads fully from start to finish", () -> {
                    var readData = new byte[FULL_DATA.length];
                    Arrays.fill(readData, (byte)0xba); // Fill array to detect that it is properly filled with NUL bytes by the read function
                    IOUtils.readFully(inputStream, readData);

                    for(int i = 0; i < 10; i++) {
                        assertThat(readData[i], is(equalTo(NUL)));
                    }
                    for(int i = 10; i < FULL_DATA.length; i++) {
                        assertThat(readData[i], is(equalTo(FULL_DATA[i])));
                    }
                    // Stream is at EOF after reading all the bytes
                    assertThat(inputStream.read(), is(equalTo(-1)));
                });

                It("skips bytes before start of the range", () -> {
                    assertThat(inputStream.skip(5), is(equalTo(5L))); // Can skip bytes before start of range

                    // Check that read data skips the 5 bytes that were skipped
                    var readData = new byte[FULL_DATA.length-5];
                    Arrays.fill(readData, (byte)0xba); // Fill array to detect that it is properly filled with NUL bytes by the read function
                    IOUtils.readFully(inputStream, readData);
                    assertThat(readData[0], is(equalTo(NUL)));
                    assertThat(readData[4], is(equalTo(NUL)));
                    for(int i = 5; i < readData.length; i++) {
                        assertThat(readData[i], is(equalTo(FULL_DATA[5+i])));
                    }

                    assertThat(inputStream.read(), is(equalTo(-1))); // EOF
                });

                It("skips bytes into the range", () -> {
                    inputStream.skipNBytes(15);

                    var readData = new byte[6];
                    IOUtils.readFully(inputStream, readData);
                    for(int i = 0; i < 6; i++) {
                        assertThat(readData[i], is(equalTo(FULL_DATA[15+i])));
                    }
                });

                It("skips bytes inside the range", () -> {
                    inputStream.skipNBytes(10); // Skip right up to the start of the range

                    assertThat(inputStream.read(), is(equalTo(FULL_DATA[10] & 0xff)));
                    inputStream.skipNBytes(2); // bytes 11 & 12 are skipped
                    assertThat(inputStream.read(), is(equalTo(FULL_DATA[13] & 0xff)));
                });

                It("skips bytes out of the range", () -> {
                    inputStream.skipNBytes(10); // Skip right up to the start of the range
                    assertThat(inputStream.read(), is(equalTo(FULL_DATA[10] & 0xff)));
                    inputStream.skipNBytes(FULL_DATA.length - 11); // Skip until the end of the range

                    assertThat(inputStream.read(), is(equalTo(-1))); // EOF
                });
            });
            Context("with a range in the middle", () -> {
                BeforeEach(() -> {
                    inputStream = PartialContentInputStream.fromContentRange(
                            new ByteArrayInputStream(FULL_DATA, 3, 4),
                            "bytes 3-6/"+FULL_DATA.length // bytes in the range description are *inclusive*
                    );
                });
                AfterEach(() -> {
                    inputStream.close();
                });
                It("reads fully from start to finish", () -> {
                    var readData = new byte[FULL_DATA.length];
                    Arrays.fill(readData, (byte)0xba); // Fill array to detect that it is properly filled with NUL bytes by the read function
                    IOUtils.readFully(inputStream, readData);

                    for(int i = 0; i < 3; i++) {
                        assertThat(readData[i], is(equalTo(NUL)));
                    }
                    assertThat(readData[3], is(equalTo(FULL_DATA[3])));
                    assertThat(readData[4], is(equalTo(FULL_DATA[4])));
                    assertThat(readData[5], is(equalTo(FULL_DATA[5])));
                    assertThat(readData[6], is(equalTo(FULL_DATA[6])));
                    for(int i = 7; i < FULL_DATA.length; i++) {
                        assertThat(readData[i], is(equalTo(NUL)));
                    }

                    // Stream is at EOF after reading all the bytes
                    assertThat(inputStream.read(), is(equalTo(-1)));
                });

                It("skips bytes before start of the range", () -> {
                    assertThat(inputStream.skip(2), is(equalTo(2L))); // Can skip bytes before start of range

                    // Check that read data skips the 2 bytes that were skipped
                    var readData = new byte[6];
                    Arrays.fill(readData, (byte)0xba); // Fill array to detect that it is properly filled with NUL bytes by the read function
                    IOUtils.readFully(inputStream, readData);
                    assertThat(readData[0], is(equalTo(NUL)));
                    assertThat(readData[1], is(equalTo(FULL_DATA[3])));
                    assertThat(readData[2], is(equalTo(FULL_DATA[4])));
                    assertThat(readData[3], is(equalTo(FULL_DATA[5])));
                    assertThat(readData[4], is(equalTo(FULL_DATA[6])));
                    assertThat(readData[5], is(equalTo(NUL)));
                });

                It("skips bytes into the range", () -> {
                    inputStream.skipNBytes(4);

                    var readData = new byte[6];
                    Arrays.fill(readData, (byte)0xba); // Fill array to detect that it is properly filled with NUL bytes by the read function
                    IOUtils.readFully(inputStream, readData);
                    assertThat(readData[0], is(equalTo(FULL_DATA[4])));
                    assertThat(readData[1], is(equalTo(FULL_DATA[5])));
                    assertThat(readData[2], is(equalTo(FULL_DATA[6])));
                    assertThat(readData[3], is(equalTo(NUL)));

                });

                It("skips bytes inside the range", () -> {
                    inputStream.skipNBytes(3); // Skip right up to the start of the range

                    assertThat(inputStream.read(), is(equalTo(FULL_DATA[3] & 0xff)));
                    inputStream.skipNBytes(2); // Bytes 4 & 5 are skipped
                    assertThat(inputStream.read(), is(equalTo(FULL_DATA[6] & 0xff)));
                    assertThat(inputStream.read(), is(equalTo(NUL & 0xff)));
                });

                It("skips bytes out of the range", () -> {
                    inputStream.skipNBytes(3); // Skip right up to the start of the range
                    assertThat(inputStream.read(), is(equalTo(FULL_DATA[3] & 0xff)));
                    inputStream.skipNBytes(FULL_DATA.length - 4); // Skip until past the end of the range; right up until the end of the data

                    assertThat(inputStream.read(), is(equalTo(-1))); // EOF
                });

                It("skips bytes after the end of the range", () -> {
                    inputStream.skipNBytes(7); // Skip right up to the end of the range

                    assertThat(inputStream.skip(Long.MAX_VALUE), is(equalTo((long)FULL_DATA.length - 7))); // All the rest of the bytes can be skipped at once
                });

            });

        });
    }


    @Test
    public void noop() {}
}