package org.springframework.content.common.renditions;

import java.io.InputStream;

public interface RenditionProvider {

    public String consumes();
    public String[] produces();
    public InputStream convert(InputStream fromInputSource, String toMimeType);
	  
}
