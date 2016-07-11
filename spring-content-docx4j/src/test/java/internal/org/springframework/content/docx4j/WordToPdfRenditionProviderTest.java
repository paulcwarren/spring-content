package internal.org.springframework.content.docx4j;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.InputStream;
import java.util.Arrays;

import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.content.common.renditions.RenditionProvider;

public class WordToPdfRenditionProviderTest {

	private RenditionProvider service;
	
	@Before
	public void setUp() {
		service = new WordToPdfRenditionProvider();
	}
	
	@Test
	public void testCanConvert() {
		assertThat(service.consumes(), is("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
		assertThat(Arrays.asList(service.produces()), hasItems("application/pdf"));
	}

	@Test
	public void testConvert() throws Exception {
		InputStream converted = service.convert(this.getClass().getResourceAsStream("/sample-docx2.docx"), 
												"application/pdf");

		String content = pdfToText(converted);
		assertThat(content, is("This is the Document Title\n \nand this is the document body.\n \n \n"));
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
				if (cosDoc != null)
					cosDoc.close();
				if (pdDoc != null)
					pdDoc.close();
			} catch (Exception e1) {
				e.printStackTrace();
			}
		}
		return null;
	}

}
