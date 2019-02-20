package internal.org.springframework.content.rest.io;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.time.ZonedDateTime;

/**
 * Represents an input stream provided by a renderer as a resource.
 *
 * TODO: refactor this into the renditions sub-system.
 */
public class RenderedResource extends InputStreamResource {

    private Resource original;
    private long lastModified;

    public RenderedResource(InputStream rendition, Resource original) {
        super(rendition);
        this.original = original;
        this.lastModified = ZonedDateTime.now().toInstant().toEpochMilli();
    }

    @Override
    public long contentLength() throws IOException {
        return -1L;
    }

    @Override
    public long lastModified() throws IOException {
        return lastModified;
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
}
