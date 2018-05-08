package org.springframework.content.renditions.renderers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.POIXMLProperties;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.content.commons.io.DefaultMediaResource;
import org.springframework.content.commons.renditions.RenditionCapability;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.renditions.RenditionException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.renditions.poi.POIService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import internal.org.springframework.renditions.poi.POIServiceImpl;

@Service
public class WordToJpegRenderer implements RenditionProvider {

	private static Log logger = LogFactory.getLog(WordToJpegRenderer.class);

	private POIService poi = null;

	public WordToJpegRenderer() {
		poi = new POIServiceImpl();
	};

	public WordToJpegRenderer(POIService poi) {
		this.poi = poi;
	}

	@Override
	public String consumes() {
		return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	}

	@Override
	public Boolean consumes(String fromMimeType) {
		if (fromMimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
			return true;
		return false;
	}

	@Override
	public String[] produces() {
		return new String[] { "image/jpeg", "image/jpg" };
	}

	@Override
	public RenditionCapability isCapable(String fromMimeType, String toMimeType) {
		if ((MimeType.valueOf(toMimeType).includes(MimeType.valueOf("image/jpg"))
				|| MimeType.valueOf(toMimeType).includes(MimeType.valueOf("image/jpeg")))
				&& MimeType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
						.includes(MimeType.valueOf(fromMimeType)))
			return RenditionCapability.GOOD_CAPABILITY;
		return RenditionCapability.NOT_CAPABLE;
	}

	@SuppressWarnings("resource")
	@Override
	public Resource convert(Resource fromInputSource, String toMimeType) {

		Assert.notNull(fromInputSource, "input source must not be null");

		XWPFDocument wordDoc = null;
		try {
			wordDoc = poi.xwpfDocument(fromInputSource.getInputStream());
		} catch (Exception e) {
			throw new RenditionException(String
					.format("Unexpected error reading input attempting to get mime-type rendition %s", toMimeType), e);
		}

		if (wordDoc != null) {
			try {
				POIXMLProperties props = wordDoc.getProperties();
				return new DefaultMediaResource(new InputStreamResource(props.getThumbnailImage()), "image/jpeg",
						props.getThumbnailFilename());
			} catch (Exception e) {
				throw new RenditionException(
						String.format("Unexpected error getting thumbnail for mime-type rendition %s", toMimeType), e);
			}
		}
		return null;
	}
}
