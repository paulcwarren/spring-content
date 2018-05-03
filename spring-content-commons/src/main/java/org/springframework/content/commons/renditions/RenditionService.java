package org.springframework.content.commons.renditions;

import org.springframework.core.io.Resource;

public interface RenditionService {

	public boolean canConvert(String fromMimeType, String toMimeType);

	public String[] conversions(String fromMimeType);

	public Resource convert(String fromMimeType, Resource fromInputSource, String toMimeType);

	public RenditionProvider getProvider(String fromMimeType, String toMimeType);
}
