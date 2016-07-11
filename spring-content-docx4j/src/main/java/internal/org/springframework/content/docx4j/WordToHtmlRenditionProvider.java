package internal.org.springframework.content.docx4j;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.docx4j.Docx4J;
import org.docx4j.Docx4jProperties;
import org.docx4j.convert.out.HTMLSettings;
import org.docx4j.convert.out.html.AbstractHtmlExporter;
import org.docx4j.convert.out.html.HtmlExporterNG2;
import org.docx4j.model.fields.FieldUpdater;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.content.common.renditions.RenditionProvider;
import org.springframework.stereotype.Service;

@Service
public class WordToHtmlRenditionProvider implements RenditionProvider {

	@Override
	public String consumes() {
		return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	}

	@Override
	public String[] produces() {
		return new String[] {"text/html"};
	}

	@Override
	public InputStream convert(InputStream fromInputSource, String toMimeType) {
		try {
			WordprocessingMLPackage pkg = WordprocessingMLPackage.load(fromInputSource);
	
			// Refresh the values of DOCPROPERTY fields 
			FieldUpdater updater = new FieldUpdater(pkg);
			updater.update(true);
			
			AbstractHtmlExporter exporter = new HtmlExporterNG2();
			HTMLSettings htmlSettings= Docx4J.createHTMLSettings();
	    	htmlSettings.setImageDirPath("/tmp/sample-docx.html_files");
	    	htmlSettings.setImageTargetUri("/tmp/_files");
	    	htmlSettings.setWmlPackage(pkg);
	    	
	    	Docx4jProperties.setProperty("docx4j.Convert.Out.HTML.OutputMethodXML", true);
	 
			OutputStream os = new FileOutputStream("/tmp/temp.html");
			Docx4J.toHTML(htmlSettings, os, Docx4J.FLAG_EXPORT_PREFER_XSL);
			IOUtils.closeQuietly(os);

			if (pkg.getMainDocumentPart().getFontTablePart()!=null) {
				pkg.getMainDocumentPart().getFontTablePart().deleteEmbeddedFontTempFiles();
			}		
			// This would also do it, via finalize() methods
			htmlSettings = null;
			pkg = null;

			return new FileInputStream("/tmp/temp.html");
		} catch (Exception e) {
			
		}
		
		return null;
	}

}
