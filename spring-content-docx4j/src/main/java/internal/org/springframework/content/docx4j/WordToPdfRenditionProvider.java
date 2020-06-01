package internal.org.springframework.content.docx4j;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.docx4j.Docx4J;
import org.docx4j.model.fields.FieldUpdater;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.content.commons.io.FileRemover;
import org.springframework.content.commons.io.ObservableInputStream;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;

import static java.lang.String.format;

@Service
public class WordToPdfRenditionProvider implements RenditionProvider {

	private static final Log logger = LogFactory.getLog(WordToPdfRenditionProvider.class);

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

			File tmpFile = Files.createTempFile(null, null, new FileAttribute<?>[]{}).toFile();
			OutputStream os = new java.io.FileOutputStream(tmpFile);

			Docx4J.toPDF(pkg, os);

			return new ObservableInputStream(new FileInputStream(tmpFile), new FileRemover(tmpFile));
		}
		catch (Exception e) {
			logger.warn(format("%s rendition failed", toMimeType), e);
		}

		return null;
	}
}
