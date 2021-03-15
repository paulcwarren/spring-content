package internal.org.springframework.content.azure.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;

import org.springframework.content.commons.io.DeletableResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;

import com.azure.storage.blob.BlobServiceClient;

public class AzureBlobResource implements WritableResource, DeletableResource {

	private BlobServiceClient client;
	private Resource delegate;
	private String bucket;

	public AzureBlobResource(BlobServiceClient client, String bucket, Resource delegate) {
		Assert.notNull(client, "client must be specified");
		Assert.hasText(bucket, "bucket must be specified");
		Assert.isInstanceOf(WritableResource.class, delegate);
		this.client = client;
		this.bucket = bucket;
		this.delegate = delegate;
	}

	public BlobServiceClient getClient() {
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
		    client.getBlobContainerClient(bucket).getBlobClient(getFilename()).delete();
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
}
