package internal.org.springframework.content.docx4j;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.docx4j.Docx4J;
import org.docx4j.fonts.PhysicalFonts;
import org.docx4j.model.fields.FieldUpdater;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

import org.springframework.content.commons.renditions.RenditionProvider;

public class WordToPdfRenditionProvider implements RenditionProvider {

	@Override
	public String consumes() {
		return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	}

	@Override
	public String[] produces() {
		return new String[] { "application/pdf" };
	}

	@Override
	public InputStream convert(InputStream fromInputSource, String toMimeType) {
		try {
			WordprocessingMLPackage pkg = WordprocessingMLPackage.load(fromInputSource);

			FieldUpdater updater = new FieldUpdater(pkg);
			updater.update(true);

			String outputfilepath;
			outputfilepath = "/tmp/temp.pdf";
			OutputStream os = new java.io.FileOutputStream(outputfilepath);

			Docx4J.toPDF(pkg, os);

			return new FileInputStream(outputfilepath);
		}
		catch (Exception e) {

		}

		return null;
	}
}
