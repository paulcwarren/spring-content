package org.springframework.content.commons.renditions;

import java.io.InputStream;

public interface RenditionProvider {

    public String consumes();
    public String[] produces();
    public InputStream convert(InputStream fromInputSource, String toMimeType);
	  
}
