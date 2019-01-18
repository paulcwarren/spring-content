package internal.org.springframework.content.rest.io;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents an input stream provided by a renderer as a resource.
 *
 * TODO: refactor this into the renditions sub-system.
 */
public class RenderedResource extends InputStreamResource {

    private Object entity;
    private Resource original;

    public RenderedResource(InputStream rendition, Object entity, Resource original) {
        super(rendition);
        this.entity = entity;
        this.original = original;
    }

    @Override
    public long contentLength() throws IOException {
        return -1L;
    }

    @Override
    public long lastModified() throws IOException {
        return original.lastModified();
    }
}
