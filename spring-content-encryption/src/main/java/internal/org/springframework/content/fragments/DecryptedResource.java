package internal.org.springframework.content.fragments;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;

/**
 * A resource that will be decrypted on-demand
 */
class DecryptedResource extends AbstractResource {

    private final InputStreamSource decryptedInputStreamSource;
    private final Resource originalResource;

    DecryptedResource(InputStreamSource decryptedInputStreamSource, Resource originalResource) {
        this.decryptedInputStreamSource = decryptedInputStreamSource;
        this.originalResource = originalResource;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return decryptedInputStreamSource.getInputStream();
    }

    @Override
    public boolean exists() {
        return originalResource.exists();
    }

    @Override
    public long contentLength() throws IOException {
        return originalResource.contentLength();
    }

    @Override
    public long lastModified() throws IOException {
        return originalResource.lastModified();
    }

    @Override
    public String getFilename() {
        return originalResource.getFilename();
    }

    @Override
    public String getDescription() {
        return "Decrypted " + originalResource.getDescription();
    }
}
