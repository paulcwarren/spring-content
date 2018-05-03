package org.springframework.content.commons.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

public class MedializedResource implements MediaResource {

	private String name;
	private String mime;
	private Resource resource;

	public MedializedResource(Resource resource, String mime, String name) {
		this.resource = resource;
		this.mime = mime;
		this.name = name;
	}

	public void resetResource(Resource r) {
		resource = r;
	}

	@Override
	public boolean exists() {
		return resource.exists();
	}

	@Override
	public URL getURL() throws IOException {
		return resource.getURL();
	}

	@Override
	public URI getURI() throws IOException {
		return resource.getURI();
	}

	@Override
	public File getFile() throws IOException {
		return resource.getFile();
	}

	@Override
	public long contentLength() throws IOException {
		if (resource instanceof InputStreamResource)
			return -1; // InputStreamResource can't determine content length without reading stream so
						// better avoid it
		return resource.contentLength();
	}

	@Override
	public long lastModified() throws IOException {
		return resource.lastModified();
	}

	@Override
	public Resource createRelative(String relativePath) throws IOException {
		return resource.createRelative(relativePath);
	}

	@Override
	public String getFilename() {
		return resource.getFilename();
	}

	@Override
	public String getDescription() {
		return resource.getDescription();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return resource.getInputStream();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getMime() {
		return mime;
	}
}
