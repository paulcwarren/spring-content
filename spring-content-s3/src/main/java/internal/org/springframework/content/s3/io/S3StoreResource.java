package internal.org.springframework.content.s3.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;

import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.io.RangeableResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

public class S3StoreResource implements WritableResource, DeletableResource, RangeableResource {

	private S3Client client;
	private Resource delegate;
	private String bucket;

	public S3StoreResource(S3Client client, String bucket, Resource delegate) {
		Assert.notNull(client, "client must be specified");
		Assert.hasText(bucket, "bucket must be specified");
		Assert.isInstanceOf(WritableResource.class, delegate);
		this.client = client;
		this.bucket = bucket;
		this.delegate = delegate;
	}

	public S3Client getClient() {
		return client;
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
	public URL getURL() throws IOException {
		return delegate.getURL();
	}

	@Override
	public URI getURI() throws IOException {
		return delegate.getURI();
	}

	@Override
	public File getFile() throws IOException {
		return delegate.getFile();
	}

	@Override
	public long contentLength() throws IOException {
		return delegate.contentLength();
	}

	@Override
	public long lastModified() throws IOException {
		return delegate.lastModified();
	}

	@Override
	public Resource createRelative(String relativePath) throws IOException {
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
	public InputStream getInputStream() throws IOException {
		return delegate.getInputStream();
	}

	@Override
	public void delete() {
		if (delegate.exists()) {
			DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
	                .bucket(bucket)
	                .key(delegate.getFilename())
	                .build();

	        client.deleteObject(deleteObjectRequest);
		}
	}

	@Override
	public boolean isWritable() {
		return ((WritableResource) delegate).isWritable();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return ((WritableResource) delegate).getOutputStream();
	}

    @Override
    public void setRange(String range) {
        ((RangeableResource)delegate).setRange(range);
    }

	/**
	 * Set the Content-Type value that will be specified as object metadata when saving resource to the object storage.
	 * @param contentType Content-Type value or null
	 */
	public void setContentType(String contentType) {
		if (delegate instanceof SimpleStorageResource) {
			((SimpleStorageResource) delegate).setContentType(contentType);
		}
	}

	/**
	 * Determine the Content-Type value of the resource as saved in object storage.
	 * @return Content-Type value of the resource
	 * @throws IOException
	 */
	public String contentType() throws IOException {
		if (delegate instanceof SimpleStorageResource) {
			return ((SimpleStorageResource) delegate).contentType();
		}
		return null;
	}
}
