package internal.org.springframework.content.docx4j;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.docx4j.TextUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.springframework.content.commons.renditions.RenditionCapability;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

@Service
public class WordToTextRenditionProvider implements RenditionProvider {

	@Override
	public String consumes() {
		return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	}

	@Override
	public Boolean consumes(String fromMimeType) {
		if (fromMimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
			return true;
		return false;
	}

	@Override
	public String[] produces() {
		return new String[] { "text/plain" };
	}

	@Override
	public RenditionCapability isCapable(String fromMimeType, String toMimeType) {
		if (MimeType.valueOf(toMimeType).includes(MimeType.valueOf("text/plain")) && consumes(fromMimeType))
			return RenditionCapability.GOOD_CAPABILITY;
		return RenditionCapability.NOT_CAPABLE;
	}

	@Override
	public Resource convert(Resource fromInputSource, String toMimeType) {
		try {
			WordprocessingMLPackage pkg = WordprocessingMLPackage.load(fromInputSource.getInputStream());

			MainDocumentPart documentPart = pkg.getMainDocumentPart();

			org.docx4j.wml.Document wmlDocumentEl = (org.docx4j.wml.Document) documentPart.getJaxbElement();

			OutputStream os = new FileOutputStream("/tmp/temp.txt");
			Writer out = new OutputStreamWriter(os);

			TextUtils.extractText(wmlDocumentEl, out);
			out.close();

			if (pkg.getMainDocumentPart().getFontTablePart() != null) {
				pkg.getMainDocumentPart().getFontTablePart().deleteEmbeddedFontTempFiles();
			}
			// This would also do it, via finalize() methods
			pkg = null;

			return new FileSystemResource("/tmp/temp.txt");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

}
