package internal.org.springframework.content.docx4j;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.springframework.content.commons.renditions.RenditionCapability;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;

public class JpegToPngRenditionProvider implements RenditionProvider {

	@Override
	public String consumes() {
		return "image/jpg";
	}

	@Override
	public Boolean consumes(String fromMimeType) {
		if (MimeType.valueOf("image/jpeg").includes(MimeType.valueOf(fromMimeType))
				|| MimeType.valueOf("image/jpg").includes(MimeType.valueOf(fromMimeType)))
			return true;
		return false;
	}

	@Override
	public String[] produces() {
		throw new UnsupportedOperationException();
	}

	@Override
	public RenditionCapability isCapable(String fromMimeType, String toMimeType) {
		if (MimeType.valueOf(toMimeType).includes(MimeType.valueOf("image/png")) && consumes(fromMimeType))
			return RenditionCapability.GOOD_CAPABILITY;
		return RenditionCapability.NOT_CAPABLE;
	}

	@Override
	public Resource convert(Resource fromInputSource, String toMimeType) {
		InputStream is = null;
		try {
			is = fromInputSource.getInputStream();
			// read a jpeg from a inputFile
			BufferedImage bufferedImage = ImageIO.read(is);

			// write the bufferedImage back to outputFile
			ImageIO.write(bufferedImage, "png", new File("/tmp/temp.png"));

			return new FileSystemResource("/tmp/temp.png");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(is);
		}
		return null;
	}

}
