package org.springframework.renditions.poi;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface PDFService {

	PDDocument load(InputStream stream) throws IOException;

	PDFRenderer renderer(PDDocument doc);

	void writeImage(BufferedImage bim, String format, OutputStream out) throws IOException;
}
