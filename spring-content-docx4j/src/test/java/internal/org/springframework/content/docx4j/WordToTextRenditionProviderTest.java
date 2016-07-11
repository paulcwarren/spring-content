package internal.org.springframework.content.docx4j;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.content.common.renditions.RenditionProvider;

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
	}

	@Test
	public void testConvert() throws Exception {
		InputStream converted = service.convert(this.getClass().getResourceAsStream("/sample-docx2.docx"), 
												"text/plain");

		String content = IOUtils.toString(converted);
		assertThat(content, is("This is the Document Title and this is the document body."));
	}

}
