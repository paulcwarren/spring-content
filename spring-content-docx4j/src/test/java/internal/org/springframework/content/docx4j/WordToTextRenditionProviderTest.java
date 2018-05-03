package internal.org.springframework.content.docx4j;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.content.commons.renditions.RenditionCapability;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class WordToTextRenditionProviderTest {

	private RenditionProvider service;

	@Before
	public void setUp() {
		service = new WordToTextRenditionProvider();
	}

	@Test
	public void testCanConvert() {
		assertThat(service.consumes(), is("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
		assertThat(Arrays.asList(service.produces()), hasItems("text/plain"));
		assertThat(service
				.isCapable("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain")
				.isBetterThan(RenditionCapability.NOT_CAPABLE), is(true));
	}

	@Test
	public void testConvert() throws Exception {
		Resource converted = service.convert(new ClassPathResource("/sample-docx2.docx"), "text/plain");

		String content = IOUtils.toString(converted.getInputStream());
		assertThat(content, is("This is the Document Title and this is the document body."));
	}

}
