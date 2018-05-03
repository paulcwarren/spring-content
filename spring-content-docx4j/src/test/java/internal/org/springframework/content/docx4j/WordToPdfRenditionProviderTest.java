package internal.org.springframework.content.docx4j;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.InputStream;

import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.content.commons.renditions.RenditionCapability;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class WordToPdfRenditionProviderTest {

	private RenditionProvider service;

	@Before
	public void setUp() {
		service = new WordToPdfRenditionProvider();
	}

	@Test
	public void testCanConvert() {
		assertThat(service
				.isCapable("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/pdf")
				.isBetterThan(RenditionCapability.NOT_CAPABLE), is(true));
	}

	@Test
	public void testConvert() throws Exception {
		Resource converted = service.convert(new ClassPathResource("/sample-docx2.docx"), "application/pdf");

		String content = pdfToText(converted.getInputStream());
		assertThat(content,
				is("This is the Document Title" + System.lineSeparator() + " " + System.lineSeparator()
						+ "and this is the document body." + System.lineSeparator() + " " + System.lineSeparator() + " "
						+ System.lineSeparator()));
	}

	private String pdfToText(InputStream in) {
		PDFParser parser = null;
		PDDocument pdDoc = null;
		COSDocument cosDoc = null;
		PDFTextStripper pdfStripper;

		try {
			parser = new PDFParser(in);
			parser.parse();
			cosDoc = parser.getDocument();
			pdfStripper = new PDFTextStripper();
			pdDoc = new PDDocument(cosDoc);
			return pdfStripper.getText(pdDoc);
			// System.out.println(parsedText.replaceAll("[^A-Za-z0-9. ]+", ""));
		} catch (Exception e) {
			e.printStackTrace();
			try {
				if (cosDoc != null) {
					cosDoc.close();
				}
				if (pdDoc != null) {
					pdDoc.close();
				}
			} catch (Exception e1) {
				e.printStackTrace();
			}
		}
		return null;
	}

}
