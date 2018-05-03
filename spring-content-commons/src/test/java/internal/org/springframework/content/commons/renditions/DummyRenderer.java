package internal.org.springframework.content.commons.renditions;

import org.springframework.content.commons.renditions.RenditionCapability;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;

public class DummyRenderer implements RenditionProvider {
	@Override
	public String consumes() {
		return "one/thing";
	}

	@Override
	public Boolean consumes(String fromMimeType) {
		if (MimeType.valueOf("one/thing").includes(MimeType.valueOf(fromMimeType)))
			return true;
		return false;
	}

	@Override
	public String[] produces() {
		return new String[] { "sometihng/else" };
	}

	@Override
	public RenditionCapability isCapable(String fromMimeType, String toMimeType) {
		if (MimeType.valueOf(toMimeType).includes(MimeType.valueOf("something/else")) && consumes(fromMimeType))
			return RenditionCapability.GOOD_CAPABILITY;
		return RenditionCapability.NOT_CAPABLE;
	}

	@SuppressWarnings("resource")
	@Override
	public Resource convert(Resource fromInputSource, String toMimeType) {

		return null;
	}
}
