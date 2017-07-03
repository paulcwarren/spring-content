package internal.org.springframework.content.rest.mappings;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

public class StoreByteRangeHttpRequestHandler extends ResourceHttpRequestHandler {

	public StoreByteRangeHttpRequestHandler() {
	}

    @Override
    protected Resource getResource(HttpServletRequest request) throws IOException {
	    return (Resource)request.getAttribute("SPRING_CONTENT_RESOURCE");
    }

	@Override
	protected MediaType getMediaType(HttpServletRequest request, Resource resource) {
		if (request.getAttribute("SPRING_CONTENT_CONTENTTYPE") != null) {
			return MediaType.valueOf((String)request.getAttribute("SPRING_CONTENT_CONTENTTYPE")); 
		}
		return super.getMediaType(request, resource);
	}
    
    
}
