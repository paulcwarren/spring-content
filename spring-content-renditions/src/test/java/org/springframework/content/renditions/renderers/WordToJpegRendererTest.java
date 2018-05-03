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
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.poi.POIXMLProperties;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.runner.RunWith;
import org.springframework.content.commons.renditions.RenditionCapability;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.renditions.RenditionException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.renditions.poi.POIService;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class WordToJpegRendererTest {

	private POIService poi;
	private XWPFDocument doc;
	POIXMLProperties props;

	private RenditionProvider renderer;

	private Resource input;
	private String mimeType;

	private Exception e;

	{
		Describe("WordToJpegRenderer", () -> {
			BeforeEach(() -> {
				poi = mock(POIService.class);
				renderer = new WordToJpegRenderer(poi);
			});
			/*
			 * Context("#consumes", () -> { It("should return word ml mimetype", () -> {
			 * assertThat(renderer.consumes(),
			 * is("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
			 * ); }); }); Context("#produces", () -> { It("should return jpeg mimetype", ()
			 * -> { assertThat(renderer.produces(), hasItemInArray("image/jpg")); }); });
			 */
			Context("#isCapable", () -> {
				It("should be able to convert word in jpg", () -> {
					assertThat(renderer
							.isCapable("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
									"image/jpg")
							.isBetterThan(RenditionCapability.NOT_CAPABLE), is(true));
					assertThat(renderer
							.isCapable("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
									"image/jpeg")
							.isBetterThan(RenditionCapability.NOT_CAPABLE), is(true));
				});
			});
			Context("#convert", () -> {
				JustBeforeEach(() -> {
					try {
						renderer.convert(input, mimeType);
					} catch (Exception e) {
						this.e = e;
					}
				});
				Context("given an input stream and a mimetype", () -> {
					BeforeEach(() -> {
						doc = mock(XWPFDocument.class);
						when(poi.xwpfDocument(anyObject())).thenReturn(doc);
						props = mock(POIXMLProperties.class);
						when(doc.getProperties()).thenReturn(props);

						input = new InputStreamResource(new ByteArrayInputStream("".getBytes()));
					});
					It("should get the embedded thumbnail from the XWPFDocument's properties", () -> {
						verify(props).getThumbnailImage();
					});
					Context("when the input stream is not a valid word file", () -> {
						BeforeEach(() -> {
							doc = mock(XWPFDocument.class);
							doThrow(NotOfficeXmlFileException.class).when(poi).xwpfDocument(anyObject());
						});
						It("should throw a RenditionException", () -> {
							assertThat(e, is(not(nullValue())));
							assertThat(e, is(instanceOf(RenditionException.class)));
						});
					});
					/*
					 * 
					 * NOTE!!! -> POIXMLDocument.getProperties does not declare throw statement
					 * 
					 * Context("when the word document fails to return properties", () -> {
					 * BeforeEach(()-> { doc = mock(XWPFDocument.class);
					 * when(poi.xwpfDocument(anyObject())).thenReturn(doc); props =
					 * mock(POIXMLProperties.class);
					 * doThrow(Exception.class).when(doc).getProperties(); });
					 * It("should throw a RenditionException", () -> { assertThat(e,
					 * is(not(nullValue()))); assertThat(e,
					 * is(instanceOf(RenditionException.class))); }); });
					 */
					Context("when the word document fails to return a thumbnail", () -> {
						BeforeEach(() -> {
							doc = mock(XWPFDocument.class);
							when(poi.xwpfDocument(anyObject())).thenReturn(doc);
							props = mock(POIXMLProperties.class);
							when(doc.getProperties()).thenReturn(props);
							doThrow(IOException.class).when(props).getThumbnailImage();
						});
						It("should throw a RenditionException", () -> {
							assertThat(e, is(not(nullValue())));
							assertThat(e, is(instanceOf(RenditionException.class)));
						});
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
