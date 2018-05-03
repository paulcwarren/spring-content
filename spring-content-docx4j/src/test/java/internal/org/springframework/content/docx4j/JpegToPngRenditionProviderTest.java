package internal.org.springframework.content.docx4j;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.content.commons.renditions.RenditionCapability;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.core.io.ClassPathResource;

public class JpegToPngRenditionProviderTest {

	private RenditionProvider service;

	@Before
	public void setUp() {
		service = new JpegToPngRenditionProvider();
	}

	@Test
	public void testCanConvert() {
		assertThat(service.isCapable("image/jpeg", "image/png").isBetterThan(RenditionCapability.NOT_CAPABLE),
				is(true));
	}

	@Test
	public void testConvert() throws Exception {
		InputStream is = service.convert(new ClassPathResource("/sample.jpeg"), "image/png").getInputStream();

		assertThat(is.available(), is(greaterThan(0)));
		assertThat(IOUtils.contentEquals(is, this.getClass().getResourceAsStream("/sample.png")), is(true));
	}

}
