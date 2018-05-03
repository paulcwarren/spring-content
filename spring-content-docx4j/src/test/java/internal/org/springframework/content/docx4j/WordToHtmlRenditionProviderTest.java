package internal.org.springframework.content.docx4j;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;
import org.springframework.content.commons.renditions.RenditionCapability;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class WordToHtmlRenditionProviderTest {

	private RenditionProvider service;

	@Before
	public void setUp() {
		service = new WordToHtmlRenditionProvider();
	}

	@Test
	public void testCanConvert() {
		assertThat(service
				.isCapable("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/html")
				.isBetterThan(RenditionCapability.NOT_CAPABLE), is(true));
	}

	@Test
	public void testConvert() throws Exception {
		Resource converted = service.convert(new ClassPathResource("/sample-docx.docx"), "text/html");

		Document doc = Jsoup.parse(converted.getInputStream(), "UTF8", "http://example.com");
		Elements htmls = doc.getElementsByTag("HTML");
		assertThat(htmls.size(), is(1));
		Element html = htmls.get(0);
		assertThat(html, is(not(nullValue())));
	}

}
