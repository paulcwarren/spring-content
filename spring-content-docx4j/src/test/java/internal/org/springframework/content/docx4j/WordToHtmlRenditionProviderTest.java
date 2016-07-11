package internal.org.springframework.content.docx4j;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.InputStream;
import java.util.Arrays;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;
import org.springframework.content.common.renditions.RenditionProvider;

public class WordToHtmlRenditionProviderTest {

	private RenditionProvider service;
	
	@Before
	public void setUp() {
		service = new WordToHtmlRenditionProvider();
	}
	
	@Test
	public void testCanConvert() {
		assertThat(service.consumes(), is("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
		assertThat(Arrays.asList(service.produces()), hasItems("text/html"));
	}

	@Test
	public void testConvert() throws Exception {
		InputStream converted = service.convert(this.getClass().getResourceAsStream("/sample-docx.docx"), 
												"text/html");
	
		Document doc = Jsoup.parse(converted, "UTF8", "http://example.com");
		Elements htmls = doc.getElementsByTag("HTML");
		assertThat(htmls.size(), is(1));
		Element html = htmls.get(0);
		assertThat(html, is(not(nullValue())));
	}

}
