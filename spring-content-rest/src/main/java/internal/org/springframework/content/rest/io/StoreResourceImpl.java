package internal.org.springframework.content.rest.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import javax.activation.MimetypesFileTypeMap;

import org.springframework.content.commons.io.DeletableResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

public class StoreResourceImpl implements Resource, StoreResource {

    private Resource delegate;

    public StoreResourceImpl(Resource delegate) {
        Assert.notNull(delegate, "Delegate cannot be null");
        this.delegate = delegate;
    }

    @Override
    public Object getETag() {

        try {
            return this.lastModified();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public MediaType getMimeType() {

        String mimeType = new MimetypesFileTypeMap().getContentType(this.getFilename());
        return MediaType.valueOf(mimeType != null ? mimeType : "");
    }

    @Override
    public InputStream getInputStream()
            throws IOException {
        return delegate.getInputStream();
    }

    @Override
    public boolean exists() {
        return delegate.exists();
    }

    @Override
    public boolean isReadable() {
        return delegate.isReadable();
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public boolean isFile() {
        return delegate.isFile();
    }

    @Override
    public URL getURL()
            throws IOException {
        return delegate.getURL();
    }

    @Override
    public URI getURI()
            throws IOException {
        return delegate.getURI();
    }

    @Override
    public File getFile()
            throws IOException {
        return delegate.getFile();
    }

    @Override
    public ReadableByteChannel readableChannel()
            throws IOException {
        return delegate.readableChannel();
    }

    @Override
    public long contentLength()
            throws IOException {
        return delegate.contentLength();
    }

    @Override
    public long lastModified()
            throws IOException {
        return delegate.lastModified();
    }

    @Override
    public Resource createRelative(String relativePath)
            throws IOException {
        return delegate.createRelative(relativePath);
    }

    @Override
    public String getFilename() {
        return delegate.getFilename();
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public boolean isWritable() {
        return ((WritableResource)delegate).isWritable();
    }

    @Override
    public OutputStream getOutputStream()
            throws IOException {
        return ((WritableResource)delegate).getOutputStream();
    }

    @Override
    public WritableByteChannel writableChannel()
            throws IOException {
        return ((WritableResource)delegate).writableChannel();
    }

    @Override
    public void delete()
            throws IOException {
        ((DeletableResource)delegate).delete();
    }
}
