package internal.org.springframework.content.rest.contentservice;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.io.RangeableResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.server.ResponseStatusException;

import internal.org.springframework.content.rest.controllers.MethodNotAllowedException;
import internal.org.springframework.content.rest.io.RenderableResource;
import internal.org.springframework.content.rest.io.RenderedResource;
import internal.org.springframework.content.rest.mappings.StoreByteRangeHttpRequestHandler;

public class StoreContentService implements ContentService {

    private static final Logger logger = LoggerFactory.getLogger(StoreContentService.class);

    private final StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler;

    public StoreContentService(StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler) {
        this.byteRangeRestRequestHandler = byteRangeRestRequestHandler;
    }

    @Override
    public void getContent(HttpServletRequest request, HttpServletResponse response, HttpHeaders headers, Resource resource, MediaType resourceType)
            throws ResponseStatusException {
        try {
            MediaType producedResourceType = null;
            List<MediaType> acceptedMimeTypes = headers.getAccept();
            if (acceptedMimeTypes.size() > 0) {

                MediaType.sortBySpecificityAndQuality(acceptedMimeTypes);
                for (MediaType acceptedMimeType : acceptedMimeTypes) {
                    if (resource instanceof RenderableResource && ((RenderableResource) resource)
                            .isRenderableAs(acceptedMimeType)) {
                        resource = new RenderedResource(((RenderableResource) resource)
                                .renderAs(acceptedMimeType), resource);
                        producedResourceType = acceptedMimeType;
                        break;
                    }
                    else if (acceptedMimeType.includes(resourceType)) {
                        producedResourceType = resourceType;
                        break;
                    }
                }

                if (producedResourceType == null) {
                    response.setStatus(HttpStatus.NOT_FOUND.value());
                    return;
                }
            }

            if (resource instanceof RangeableResource) {
                this.configureResourceForByteRangeRequest((RangeableResource)resource, headers);
            }

            request.setAttribute("SPRING_CONTENT_RESOURCE", resource);
            request.setAttribute("SPRING_CONTENT_CONTENTTYPE", producedResourceType);
        } catch (Exception e) {

            logger.error("Unable to retrieve content", e);

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, format("Failed to handle request for %s", resource.getDescription()), e);
        }

        try {
            byteRangeRestRequestHandler.handleRequest(request, response);
        }
        catch (Exception e) {

            if (ContentStoreContentService.isClientAbortException(e)) {
                // suppress
            } else {
                logger.error("Unable to handle request", e);

                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, format("Failed to handle request for %s", resource.getDescription()), e);
            }
        }
    }

    @Override
    public void setContent(HttpServletRequest request, HttpServletResponse response, HttpHeaders headers, Resource source, MediaType sourceMimeType, Resource target)
            throws IOException, MethodNotAllowedException {

        InputStream in = source.getInputStream();
        OutputStream out = ((WritableResource) target).getOutputStream();
        IOUtils.copy(in, out);
        IOUtils.closeQuietly(out);
        IOUtils.closeQuietly(in);

    }

    @Override
    public void unsetContent(Resource resource) {

        Assert.notNull(resource, "resource must not be null");
        if (resource instanceof DeletableResource) {
            try {
                ((DeletableResource) resource).delete();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void configureResourceForByteRangeRequest(RangeableResource resource, HttpHeaders headers) {
        if (headers.containsKey(HttpHeaders.RANGE)) {
            resource.setRange(headers.getFirst(HttpHeaders.RANGE));
        }
    }
}
