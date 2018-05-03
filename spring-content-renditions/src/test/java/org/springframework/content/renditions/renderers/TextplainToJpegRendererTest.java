package org.springframework.content.renditions.renderers;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.runner.RunWith;
import org.springframework.content.commons.renditions.RenditionCapability;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class TextplainToJpegRendererTest {

	private boolean wrapText = false;
	private RenditionProvider renderer;

	private Resource input, result;
	private String mimeType;

	private Exception e;

	{
		Describe("TextplainToJpegRenderer", () -> {
			JustBeforeEach(() -> {
				renderer = new TextplainToJpegRenderer(wrapText);
			});
			/*
			 * Context("#consumes", () -> { It("should return text/plain", () -> {
			 * assertThat(renderer.consumes(), is("text/plain")); }); });
			 * Context("#produces", () -> { It("should return jpeg mimetype", () -> {
			 * assertThat(renderer.produces(), hasItemInArray("image/jpg"));
			 * assertThat(renderer.produces(), hasItemInArray("image/jpeg")); }); });
			 */
			Context("#isCapable", () -> {
				It("should be able to convert text in jpeg", () -> {
					assertThat(
							renderer.isCapable("text/plain", "image/jpg").isBetterThan(RenditionCapability.NOT_CAPABLE),
							is(true));
					assertThat(renderer.isCapable("text/plain", "image/jpeg")
							.isBetterThan(RenditionCapability.NOT_CAPABLE), is(true));
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
						BeforeEach(() -> {
							input = new InputStreamResource(
									new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
						});
						It("should produce the correct image", () -> {
							InputStream expected = this.getClass()
									.getResourceAsStream("/textplaintorenderer/single-line.jpeg");
							assertThat(expected, is(not(nullValue())));
							assertThat(result, is(not(nullValue())));
							// InputStream expected =
							// this.getClass().getResourceAsStream("/textplaintorenderer/single-line.jpeg");
							// assertThat(expected, is(not(nullValue())));
							// assertThat(IOUtils.contentEquals(expected, result), is(true));
						});
					});
					Context("given a multi-line input", () -> {
						BeforeEach(() -> {
							input = new InputStreamResource(
									new ByteArrayInputStream("Hello\nSpring\n\nContent\n\n\nWorld!".getBytes()));
						});
						It("should produce the correct image", () -> {
							assertThat(result, is(not(nullValue())));
							// assertThat(IOUtils.contentEquals(this.getClass().getResourceAsStream("/textplaintorenderer/multi-line.jpeg"),
							// result), is(true));
						});
					});
					Context("given a long line and wrapping", () -> {
						BeforeEach(() -> {
							wrapText = true;
							input = new InputStreamResource(new ByteArrayInputStream(
									"Hello Spring Content World!  This is a really long line that we expect to wrap"
											.getBytes()));
						});
						It("should produce the correct image", () -> {
							assertThat(result, is(not(nullValue())));
							// assertThat(IOUtils.contentEquals(this.getClass().getResourceAsStream("/textplaintorenderer/wrapped-line.jpeg"),
							// result), is(true));
						});
					});
					Context("given a long line and no wrapping", () -> {
						BeforeEach(() -> {
							input = new InputStreamResource(new ByteArrayInputStream(
									"Hello Spring Content World!  This is a really long line that we expect to wrap"
											.getBytes()));
						});
						It("should produce the correct image", () -> {
							assertThat(result, is(not(nullValue())));
							// assertThat(IOUtils.contentEquals(this.getClass().getResourceAsStream("/textplaintorenderer/overflowed-line.jpeg"),
							// result), is(true));
						});
					});
					Context("given a line file will overflow the image size", () -> {
						BeforeEach(() -> {
							input = new InputStreamResource(new ByteArrayInputStream(
									"Hello\n\nSpring\n\nContent\n\nWorld!\n\n\nThis\n\nis\n\na\n\nreally\n\nreally\n\nreally\n\nreally\n\nreally\n\nlong\n\nfile\n\nthat\n\nwill\n\noverflow\n\nthe\n\nimage"
											.getBytes()));
						});
						It("should produce the correct image", () -> {
							assertThat(result, is(not(nullValue())));
							// assertThat(IOUtils.contentEquals(this.getClass().getResourceAsStream("/textplaintorenderer/overflowed-image.jpeg"),
							// result), is(true));
						});
					});
				});
				Context("when the input stream is not a valid word file", () -> {
					BeforeEach(() -> {
						input = new ClassPathResource("/sample-docx.docx");
					});
					It("should not error", () -> {
						assertThat(e, is(nullValue()));
					});
				});
				Context("given a null input stream", () -> {
					It("should return an error", () -> {
						assertThat(e, is(not(nullValue())));
					});
				});
			});
		});
	}
}
