package internal.org.springframework.content.jpa.repository;

import java.io.File;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;

public class ContextFileSystemResourceLoader extends FileSystemResourceLoader {

	private File fileSystemRoot;
	
	public ContextFileSystemResourceLoader(File fileSystemRoot) {
		this.fileSystemRoot = fileSystemRoot;
	}
	
	@Override
	public Resource getResource(String location) {
		if (location.startsWith(ResourceUtils.FILE_URL_PREFIX)) {
			String path = location.substring(ResourceUtils.FILE_URL_PREFIX.length());
			return new FileSystemResource(new File(path));
		} else {
			return super.getResource(location);
		}
	}
}
