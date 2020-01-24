package internal.org.springframework.content.rest.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.time.ZonedDateTime;

import org.apache.commons.io.IOUtils;

import org.springframework.content.commons.renditions.Renderable;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import org.springframework.web.servlet.resource.HttpResource;

/**
 * Represents an input stream provided by a renderer as a resource.
 *
 * TODO: refactor this into the renditions sub-system.
 */
public class RenderableResourceImpl implements Resource, HttpResource, RenderableResource {

    private final Renderable renderer;
    private final AssociatedResource original;
    private final long lastModified;

    public RenderableResourceImpl(Renderable renderer, AssociatedResource original) {
        this.renderer = renderer;
        this.original = original;
        this.lastModified = ZonedDateTime.now().toInstant().toEpochMilli();
    }

    @Override
    public boolean isRenderableAs(MimeType mimeType) {
        InputStream is = renderer.getRendition(original.getAssociation(), mimeType.toString());
        try {
            return is != null;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @Override
    public InputStream renderAs(MimeType mimeType) {
        return renderer.getRendition(original.getAssociation(), mimeType.toString());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return original.getInputStream();
    }

    @Override
    public long contentLength() throws IOException {
        return original.contentLength();
    }

    @Override
    public long lastModified() throws IOException {
        return original.lastModified();
    }

    @Override
    public boolean exists() {
        return original.exists();
    }

    @Override
    public boolean isReadable() {
        return original.isReadable();
    }

    @Override
    public boolean isOpen() {
        return original.isOpen();
    }

    @Override
    public boolean isFile() {
        return original.isFile();
    }

    @Override
    public URL getURL() throws IOException {
        return original.getURL();
    }

    @Override
    public URI getURI() throws IOException {
        return original.getURI();
    }

    @Override
    public File getFile() throws IOException {
        return original.getFile();
    }

    @Override
    public ReadableByteChannel readableChannel() throws IOException {
        return original.readableChannel();
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
        return original.createRelative(relativePath);
    }

    @Override
    @Nullable
    public String getFilename() {
        return original.getFilename();
    }

    @Override
    public String getDescription() {
        return original.getDescription();
    }

    @Override
    public HttpHeaders getResponseHeaders() {
        if (original instanceof HttpResource) {
            return ((HttpResource)original).getResponseHeaders();
        } else {
          return new HttpHeaders();  
        }
    }
}
