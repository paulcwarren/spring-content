package org.springframework.content.renditions.renderers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

@Service
public class PdfToJpegRenderer implements RenditionProvider {

	private static Log logger = LogFactory.getLog(PdfToJpegRenderer.class);

	public PdfToJpegRenderer() {
	};

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

//		XWPFDocument wordDoc = null;
//		try {
//			wordDoc = poi.xwpfDocument(fromInputSource);
//		}
//		catch (Exception e) {
//			throw new RenditionException(String.format(
//					"Unexpected error reading input attempting to get mime-type rendition %s",
//					toMimeType), e);
//		}
//
//		if (wordDoc != null) {
//			try {
//				POIXMLProperties props = wordDoc.getProperties();
//				return props.getThumbnailImage();
//			}
//			catch (Exception e) {
//				throw new RenditionException(String.format(
//						"Unexpected error getting thumbnail for mime-type rendition %s",
//						toMimeType), e);
//			}
//		}

		PDDocument document = null;
		try {
			document = PDDocument.load(fromInputSource);
			PDFRenderer pdfRenderer = new PDFRenderer(document);
			if (document.getNumberOfPages() > 0) {
				BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 300, ImageType.RGB);
				PipedInputStream in = new PipedInputStream();
				PipedOutputStream out = new PipedOutputStream(in);
				new Thread(
						() -> {
                            try {
                                ImageIOUtil.writeImage(bim, "jpeg", out);
                            } catch (IOException e) {
                                logger.error("Error writing buffered image to piped output stream");
                            }
                        }
				).start();
				return in;
			}
		} catch (IOException e) {
			logger.error("Error rendering application/pdf to image/jpeg");
		} finally {
			try {
				document.close();
			} catch (IOException e) {
				// silent
			}
		}

		return null;
	}
}
