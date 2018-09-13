package org.springframework.content.renditions.renderers;

import internal.org.springframework.renditions.pdf.PDFServiceImpl;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.io.ScratchFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.springframework.content.commons.io.FileRemover;
import org.springframework.content.commons.io.ObservableInputStream;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.renditions.RenditionException;
import org.springframework.renditions.poi.PDFService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

@Service
public class PdfToJpegRenderer implements RenditionProvider {

	private static Log logger = LogFactory.getLog(PdfToJpegRenderer.class);

	private PDFService pdfService;

	public PdfToJpegRenderer() {
		this.pdfService = new PDFServiceImpl();
	};

	public PdfToJpegRenderer(PDFService pdfService) {
		this.pdfService = pdfService;
	}

	@Override
	public String consumes() {
		return "application/pdf";
	}

	@Override
	public String[] produces() {
		return new String[] { "image/jpg" };
	}

	@SuppressWarnings("resource")
	@Override
	public InputStream convert(InputStream fromInputSource, String toMimeType) {

		Assert.notNull(fromInputSource, "input source must not be null");

		PDDocument document = null;
		try {
			document = pdfService.load(fromInputSource);
			PDFRenderer pdfRenderer = pdfService.renderer(document);
			if (document.getNumberOfPages() > 0) {
  				BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 300, ImageType.RGB);
  				File tmp = File.createTempFile("pdftojpegrenderer", ".tmp");
  				tmp.deleteOnExit();
				FileOutputStream out = new FileOutputStream(tmp);
				try {
					pdfService.writeImage(bim, "jpeg", out);
				} catch (IOException e) {
					logger.error("Error writing buffered image to output stream");
					throw new RenditionException("Error writing buffered image to output stream", e);
				} finally {
					IOUtils.closeQuietly(out);
				}
				return new ObservableInputStream(new FileInputStream(tmp), new FileRemover(tmp));
			}
		} catch (IOException e) {
			logger.error("Error rendering application/pdf to image/jpeg");
			throw new RenditionException("Error rendering application/pdf to image/jpeg", e);
		} finally {
			try {
				if (document != null) {
					document.close();
				}
			} catch (IOException e) {
				// silent
			}
		}

		return null;
	}
}
