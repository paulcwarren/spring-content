package internal.org.springframework.content.docx4j;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.docx4j.Docx4J;
import org.docx4j.convert.out.FOSettings;
import org.docx4j.fonts.PhysicalFonts;
import org.docx4j.model.fields.FieldUpdater;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.content.common.renditions.RenditionProvider;

public class WordToPdfRenditionProvider implements RenditionProvider {

	@Override
	public String consumes() {
		return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	}

	@Override
	public String[] produces() {
		return new String[] {"application/pdf"};
	}

	@Override
	public InputStream convert(InputStream fromInputSource, String toMimeType) {
		try {
			// Font regex (optional)
			// Set regex if you want to restrict to some defined subset of fonts
			// Here we have to do this before calling createContent,
			// since that discovers fonts
			String regex = null;
			// Windows:
			// String
			// regex=".*(calibri|camb|cour|arial|symb|times|Times|zapf).*";
			//regex=".*(calibri|camb|cour|arial|times|comic|georgia|impact|LSANS|pala|tahoma|trebuc|verdana|symbol|webdings|wingding).*";
			// Mac
			// String
			// regex=".*(Courier New|Arial|Times New Roman|Comic Sans|Georgia|Impact|Lucida Console|Lucida Sans Unicode|Palatino Linotype|Tahoma|Trebuchet|Verdana|Symbol|Webdings|Wingdings|MS Sans Serif|MS Serif).*";
			PhysicalFonts.setRegex(regex);
			
			WordprocessingMLPackage pkg = WordprocessingMLPackage.load(fromInputSource);
	
			// Refresh the values of DOCPROPERTY fields 
			FieldUpdater updater = new FieldUpdater(pkg);
			updater.update(true);
			
			// FO exporter setup (required)
			// .. the FOSettings object
	    	FOSettings foSettings = Docx4J.createFOSettings();
//			if (false) {
//				foSettings.setFoDumpFile(new java.io.File("/tmp/test.fo"));
//			}
			foSettings.setWmlPackage(pkg);
			
			//ByteArrayOutputStream os = new ByteArrayOutputStream();
			
			String outputfilepath;
			outputfilepath = "/tmp/temp.pdf";
			OutputStream os = new java.io.FileOutputStream(outputfilepath);
	
			// Specify whether PDF export uses XSLT or not to create the FO
			// (XSLT takes longer, but is more complete).
			
			// Don't care what type of exporter you use
			Docx4J.toFO(foSettings, os, Docx4J.FLAG_EXPORT_PREFER_XSL);
			
			if (pkg.getMainDocumentPart().getFontTablePart()!=null) {
				pkg.getMainDocumentPart().getFontTablePart().deleteEmbeddedFontTempFiles();
			}
			
			return new FileInputStream(outputfilepath);
		} catch (Exception e) {
			
		}
		
		return null;
	}
}
