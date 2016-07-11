package internal.org.springframework.content.docx4j;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.springframework.content.common.renditions.RenditionProvider;

public class JpegToPngRenditionProvider implements RenditionProvider {

	@Override
	public String consumes() {
		return "image/jpeg";
	}

	@Override
	public String[] produces() {
		return new String[] {"image/png"};
	}

	@Override
	public InputStream convert(InputStream fromInputSource, String toMimeType) {
		try {
			// read a jpeg from a inputFile
			BufferedImage bufferedImage = ImageIO.read(fromInputSource);
	
			// write the bufferedImage back to outputFile
			ImageIO.write(bufferedImage, "png", new File("/tmp/temp.png"));
	
			return new FileInputStream("/tmp/temp.png");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(fromInputSource);
		}
		return null;
	}
}
