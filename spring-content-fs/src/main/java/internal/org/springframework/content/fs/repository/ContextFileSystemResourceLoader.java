package internal.org.springframework.content.fs.repository;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;

public class ContextFileSystemResourceLoader extends FileSystemResourceLoader {

	private static Log logger = LogFactory.getLog(ContextFileSystemResourceLoader.class);
	
	public ContextFileSystemResourceLoader(File fileSystemRoot) {
		logger.info(String.format("File system context root set to: %s", fileSystemRoot.toString()));
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
