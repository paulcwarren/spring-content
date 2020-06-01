package internal.org.springframework.content.docx4j;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.io.FileRemover;
import org.springframework.content.commons.io.ObservableInputStream;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;

import static java.lang.String.format;

@Service
public class JpegToPngRenditionProvider implements RenditionProvider {

	private static final Log logger = LogFactory.getLog(JpegToPngRenditionProvider.class);

	@Override
	public String consumes() {
		return "image/jpeg";
	}

	@Override
	public String[] produces() {
		return new String[] { "image/png" };
	}

	@Override
	public InputStream convert(InputStream fromInputSource, String toMimeType) {
		try {
			File tmpFile = Files.createTempFile(null, null, new FileAttribute<?>[]{}).toFile();
			BufferedImage bufferedImage = ImageIO.read(fromInputSource);
			ImageIO.write(bufferedImage, "png", tmpFile);
			return new ObservableInputStream(new FileInputStream(tmpFile), new FileRemover(tmpFile));
		}
		catch (Exception e) {
			logger.warn(format("%s rendition failed", toMimeType), e);
		}
		finally {
			IOUtils.closeQuietly(fromInputSource);
		}
		return null;
	}
}
