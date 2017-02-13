package internal.org.springframework.content.fs.operations;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.operations.AbstractResourceTemplate;
import org.springframework.content.commons.operations.ContentOperations;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import internal.org.springframework.content.fs.config.FilesystemProperties;
import internal.org.springframework.content.fs.repository.ContextFileSystemResourceLoader;

/**
 * {@link ContentOperations} implementation to store and retrieve file-based content.
 */
public class FileResourceTemplate extends AbstractResourceTemplate implements ContentOperations {

	private static Log logger = LogFactory.getLog(FileResourceTemplate.class);
	
	private File fileSystemRoot;

	@Autowired
	public FileResourceTemplate(FilesystemProperties props) {
		super(new ContextFileSystemResourceLoader(props.getFilesystemRoot()));
		this.fileSystemRoot = new File(props.getFilesystemRoot());
	}

	@Override
	protected String getLocation(Object contentId) {
		return new File(fileSystemRoot, contentId.toString()).toURI().toString();
	}

	@Override
	protected void deleteResource(Resource resource) throws Exception {
		if (resource != null && resource instanceof FileSystemResource) {
			if (((FileSystemResource)resource).getFile().delete()) {
				logger.debug(String.format("Deleted resource %s", resource.getFile().getPath()));
			}
		}
	}
}
