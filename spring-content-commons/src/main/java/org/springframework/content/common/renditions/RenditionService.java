package org.springframework.content.common.renditions;

import java.io.InputStream;

public interface RenditionService {
	
    public boolean canConvert(String fromMimeType, String toMimeType);
    public String[] conversions(String fromMimeType);
    public InputStream convert(String fromMimeType, InputStream fromInputSource, String toMimeType);

}
