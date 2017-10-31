package org.springframework.content.renditions.renderers;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.apache.commons.io.IOUtils;
import org.junit.runner.RunWith;
import org.springframework.content.commons.renditions.RenditionProvider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

//@RunWith(Ginkgo4jRunner.class)
//@Ginkgo4jConfiguration(threads=1)
public class TextplainToJpegRendererTest {

    private boolean wrapText = false;
    private RenditionProvider renderer;

    private InputStream input, result;
    private String mimeType;

    private Exception e;

    {
        Describe("TextplainToJpegRenderer", () -> {
            JustBeforeEach(() -> {
                renderer = new TextplainToJpegRenderer(wrapText);
            });
            Context("#consumes", () -> {
                It("should return text/plain", () -> {
                    assertThat(renderer.consumes(), is("text/plain"));
                });
            });
            Context("#produces", () -> {
                It("should return jpeg mimetype", () -> {
                    assertThat(renderer.produces(), hasItemInArray("image/jpg"));
                });
            });
            Context("#convert", () -> {
                JustBeforeEach(() -> {
                    try {
                        result = renderer.convert(input, mimeType);
                    } catch (Exception e) {
                        this.e = e;
                    }
                });
                Context("given a plain/text input", () -> {
                    Context("given a single-line input", () -> {
                        BeforeEach(()-> {
                            input = new ByteArrayInputStream("Hello Spring Content World!".getBytes());
                        });
                        It("should produce the correct image", () -> {
                            InputStream expected = this.getClass().getResourceAsStream("/textplaintorenderer/single-line.jpeg");
                            assertThat(expected, is(not(nullValue())));
                            assertThat(result, is(not(nullValue())));
                            assertThat(IOUtils.contentEquals(expected, result), is(true));
                        });
                    });
                    Context("given a multi-line input", () -> {
                        BeforeEach(()-> {
                            input = new ByteArrayInputStream("Hello\nSpring\n\nContent\n\n\nWorld!".getBytes());
                        });
                        It("should produce the correct image", () -> {
                            assertThat(IOUtils.contentEquals(this.getClass().getResourceAsStream("/textplaintorenderer/multi-line.jpeg"), result), is(true));
                        });
                    });
                    Context("given a long line and wrapping", () -> {
                        BeforeEach(()-> {
                            wrapText = true;
                            input = new ByteArrayInputStream("Hello Spring Content World!  This is a really long line that we expect to wrap".getBytes());
                        });
                        It("should produce the correct image", () -> {
                            assertThat(IOUtils.contentEquals(this.getClass().getResourceAsStream("/textplaintorenderer/wrapped-line.jpeg"), result), is(true));
                        });
                    });
                    Context("given a long line and no wrapping", () -> {
                        BeforeEach(()-> {
                            input = new ByteArrayInputStream("Hello Spring Content World!  This is a really long line that we expect to wrap".getBytes());
                        });
                        It("should produce the correct image", () -> {
                            assertThat(IOUtils.contentEquals(this.getClass().getResourceAsStream("/textplaintorenderer/overflowed-line.jpeg"), result), is(true));
                        });
                    });
                    Context("given a line file will overflow the image size", () -> {
                        BeforeEach(()-> {
                            input = new ByteArrayInputStream("Hello\n\nSpring\n\nContent\n\nWorld!\n\n\nThis\n\nis\n\na\n\nreally\n\nreally\n\nreally\n\nreally\n\nreally\n\nlong\n\nfile\n\nthat\n\nwill\n\noverflow\n\nthe\n\nimage".getBytes());
                        });
                        It("should produce the correct image", () -> {
                            assertThat(IOUtils.contentEquals(this.getClass().getResourceAsStream("/textplaintorenderer/overflowed-image.jpeg"), result), is(true));
                        });
                    });
                });
                Context("when the input stream is not a valid word file", () -> {
                    BeforeEach(()-> {
                        input = this.getClass().getResourceAsStream("/sample-docx.docx");
                    });
                    It("should not error", () -> {
                        assertThat(e, is(nullValue()));
                    });
                });
                Context("given a null input stream", () -> {
                    It("should get the embedded thumbnail from the XWPFDocument's properties", () -> {
                        assertThat(e, is(not(nullValue())));
                    });
                });
            });
        });
    }
}
