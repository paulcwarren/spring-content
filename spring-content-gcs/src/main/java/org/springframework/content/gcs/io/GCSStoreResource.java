/**
 * 
 */
package org.springframework.content.gcs.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;

import org.springframework.content.commons.io.DeletableResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;

/**
 * @author sandip.bhoi
 *
 */
public class GCSStoreResource implements WritableResource, DeletableResource {

	@Override
	public boolean exists() {
		System.out.println("In method ");
		return false;
	}

	@Override
	public URL getURL() throws IOException {
		System.out.println("In method ");
		return null;
	}

	@Override
	public URI getURI() throws IOException {
		System.out.println("In method ");
		return null;
	}

	@Override
	public File getFile() throws IOException {
		System.out.println("In method ");
		return null;
	}

	@Override
	public long contentLength() throws IOException {
		System.out.println("In method ");
		return 0;
	}

	@Override
	public long lastModified() throws IOException {
		System.out.println("In method ");
		return 0;
	}

	@Override
	public Resource createRelative(String relativePath) throws IOException {
		System.out.println("In method ");
		return null;
	}

	@Override
	public String getFilename() {
		System.out.println("In method ");
		return null;
	}

	@Override
	public String getDescription() {
		System.out.println("In method ");
		return null;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		System.out.println("In method ");
		return null;
	}

	@Override
	public void delete() throws IOException {
		System.out.println("In method ");
		
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		System.out.println("In method ");
		return null;
	}
}
