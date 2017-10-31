package org.springframework.content.renditions.renderers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.renditions.RenditionException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;

@Service
public class TextplainToJpegRenderer implements RenditionProvider {

    private static Log logger = LogFactory.getLog(WordToJpegRenderer.class);

    private static int width = 272;
    private static int margin = 5;
    private static String fontName = "Courier New";
    private static int fontSize = 12;

    private boolean wrapText = false;

    public TextplainToJpegRenderer() {}

    public TextplainToJpegRenderer(boolean wrapText) {
        this.wrapText = wrapText;
    }

    @Override
    public String consumes() {
        return "text/plain";
    }

    @Override
    public String[] produces() {
        return new String[] {"image/jpg"};
    }

    @Override
    public InputStream convert(InputStream fromInputSource, String toMimeType) {

        Assert.notNull(fromInputSource, "input source must not be null");

        Font font = null;
        try {
            font = new Font(fontName, Font.PLAIN, (int) fontSize);
        } catch (Exception e) {
            throw new RenditionException("Error creating font", e);
        }

        BufferedImage tempBuffer = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = tempBuffer.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(font);

        FontRenderContext fc = g.getFontRenderContext();
        Rectangle2D bounds = font.getStringBounds("Random Text", fc);
        int lineHeight = (int)bounds.getHeight();

        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(fromInputSource));
        } catch (Exception e) {
            throw new RenditionException("Error opening input stream", e);
        }

        ArrayList images = new ArrayList();

        int lineCnt = margin + margin;

        while (true) {
            String line;

            try {
                line = reader.readLine();
            } catch (IOException ignore) {
                break;
            }

            if (line == null) { // EOF
                break;
            }

            if ("".equals(line)) { // Empty line
                line = " ";
            }

            AttributedString attribString = new AttributedString(line);
            attribString.addAttribute(TextAttribute.BACKGROUND, Color.WHITE, 0, line.length());
            attribString.addAttribute(TextAttribute.FOREGROUND, Color.BLACK, 0, line.length());
            attribString.addAttribute(TextAttribute.FONT, font, 0, line.length());

            AttributedCharacterIterator aci = attribString.getIterator();
            LineBreakMeasurer lbm = new LineBreakMeasurer(aci, fc);

            while (lbm.getPosition() < line.length()) {
                BufferedImage lineBuffer = new BufferedImage(width, lineHeight, BufferedImage.TYPE_INT_ARGB);

                Graphics2D g1 = lineBuffer.createGraphics();
                g1.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                TextLayout layout = lbm.nextLayout(width - margin);

                int y = (int) layout.getAscent();

                layout.draw(g1, margin, y);
                images.add(lineBuffer);
                lineCnt += lineHeight;

                if (lineCnt + lineHeight > 480 || !wrapText) {
                    break;
                }
            }
        }

        if (lineCnt != 0) {
            try {
                saveImage(images, lineHeight, "/tmp/textToJpeg.jpeg");
                return new FileInputStream("/tmp/textToJpeg.jpeg");
            } catch (IOException e) {
                throw new RenditionException("Error writing image", e);
            }
        }

        return null;
    }

    private void saveImage(ArrayList<BufferedImage> images, int lineHeight, String fileName) throws IOException {
        BufferedImage buffer = new BufferedImage(272, 480, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = buffer.createGraphics();
        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, 272, 480);

        for (int i = 0; i < images.size(); i++) {
            g.drawImage((BufferedImage) images.get(i), 0, margin + (i * lineHeight), null);
        }

        OutputStream out = new FileOutputStream(new File(fileName.toString()));
        ImageIO.write(buffer, "jpg", out);
        out.close();
    }
}
