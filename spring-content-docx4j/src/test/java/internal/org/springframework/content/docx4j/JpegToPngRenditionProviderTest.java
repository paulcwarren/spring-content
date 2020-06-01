package internal.org.springframework.content.docx4j;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.content.commons.io.FileRemover;
import org.springframework.content.commons.io.ObservableInputStream;
import org.springframework.content.commons.renditions.RenditionProvider;

import internal.org.springframework.content.docx4j.JpegToPngRenditionProvider;

public class JpegToPngRenditionProviderTest {

	private RenditionProvider service;

	@Before
	public void setUp() {
		service = new JpegToPngRenditionProvider();
	}

	@Test
	public void testCanConvert() {
		assertThat(service.consumes(), is("image/jpeg"));
		assertThat(Arrays.asList(service.produces()), hasItems("image/png"));
	}

	@Test
	public void testConvert() throws Exception {
		InputStream converted = service.convert(
				this.getClass().getResourceAsStream("/sample.jpeg"), "image/png");

		assertThat(converted.available(), is(greaterThan(0)));
		assertThat(((ObservableInputStream)converted).getObservers(), hasItem(is(instanceOf(FileRemover.class))));
		assertThat(IOUtils.contentEquals(converted, this.getClass().getResourceAsStream("/sample.png")), is(true));
	}

}
