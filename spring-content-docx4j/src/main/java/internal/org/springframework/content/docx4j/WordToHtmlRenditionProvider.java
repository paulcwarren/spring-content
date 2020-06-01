package internal.org.springframework.content.docx4j;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.docx4j.Docx4J;
import org.docx4j.Docx4jProperties;
import org.docx4j.convert.out.HTMLSettings;
import org.docx4j.convert.out.html.AbstractHtmlExporter;
import org.docx4j.convert.out.html.HtmlExporterNG2;
import org.docx4j.model.fields.FieldUpdater;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.content.commons.io.FileRemover;
import org.springframework.content.commons.io.ObservableInputStream;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;

import static java.lang.String.format;

@Service
public class WordToHtmlRenditionProvider implements RenditionProvider {

	private static final Log logger = LogFactory.getLog(WordToHtmlRenditionProvider.class);

	@Override
	public String consumes() {
		return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	}

	@Override
	public String[] produces() {
		return new String[] { "text/html" };
	}

	@Override
	public InputStream convert(InputStream fromInputSource, String toMimeType) {
		try {
			WordprocessingMLPackage pkg = WordprocessingMLPackage.load(fromInputSource);

			FieldUpdater updater = new FieldUpdater(pkg);
			updater.update(true);

			AbstractHtmlExporter exporter = new HtmlExporterNG2();
			HTMLSettings htmlSettings = Docx4J.createHTMLSettings();
			htmlSettings.setWmlPackage(pkg);

			Docx4jProperties.setProperty("docx4j.Convert.Out.HTML.OutputMethodXML", true);

			File tmpFile = Files.createTempFile(null, null, new FileAttribute<?>[]{}).toFile();
			OutputStream os = new FileOutputStream(tmpFile);

			Docx4J.toHTML(htmlSettings, os, Docx4J.FLAG_EXPORT_PREFER_XSL);
			IOUtils.closeQuietly(os);

			if (pkg.getMainDocumentPart().getFontTablePart() != null) {
				pkg.getMainDocumentPart().getFontTablePart().deleteEmbeddedFontTempFiles();
			}

			htmlSettings = null;
			pkg = null;

			return new ObservableInputStream(new FileInputStream(tmpFile), new FileRemover(tmpFile));
		}
		catch (Exception e) {
			logger.warn(format("%s rendition failed", toMimeType), e);
		}

		return null;
	}

}
