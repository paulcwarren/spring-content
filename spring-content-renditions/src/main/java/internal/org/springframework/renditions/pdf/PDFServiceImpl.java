package internal.org.springframework.renditions.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.springframework.renditions.poi.PDFService;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PDFServiceImpl implements PDFService {

    @Override
    public PDDocument load(InputStream stream) throws IOException {
        return PDDocument.load(stream);
    }

    @Override
    public PDFRenderer renderer(PDDocument doc) {
        return new PDFRenderer(doc);
    }

    @Override
    public void writeImage(BufferedImage bim, String format, OutputStream out) throws IOException {
        ImageIOUtil.writeImage(bim, "jpeg", out);
    }
}
