package internal.org.springframework.content.docx4j;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.content.common.renditions.RenditionProvider;

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
		InputStream converted = service.convert(this.getClass().getResourceAsStream("/sample.jpeg"), 
												"image/png");

		assertThat(converted.available(), is(greaterThan(0)));
		assertThat(IOUtils.contentEquals(converted, this.getClass().getResourceAsStream("/sample.png")), is(true));
	}

}
