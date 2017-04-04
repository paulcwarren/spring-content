package internal.org.springframework.content.fs.operations;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.operations.AbstractResourceTemplate;
import org.springframework.content.commons.operations.ContentOperations;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import internal.org.springframework.content.fs.repository.ContextFileSystemResourceLoader;

/**
 * {@link ContentOperations} implementation to store and retrieve file-based content.
 */
public class FileResourceTemplate extends AbstractResourceTemplate {

	private static Log logger = LogFactory.getLog(FileResourceTemplate.class);
	
	@Autowired
	public FileResourceTemplate(ContextFileSystemResourceLoader loader) {
		super(loader);
	}

	// TODO: remove this method once placement strategy is fully implemented
	@Override
	public String getLocation(Object contentId) {
		return null;
	}

	public Resource create(String location) {
		Resource resource = this.get(location);
		
		File resourceFile;
		try {
			resourceFile = resource.getFile();
			File resourceParent = resourceFile.getParentFile();
			resourceParent.mkdirs();
		} catch (IOException ioe) {
			logger.error(String.format("Unexpected error creating resource %s", location), ioe);
		}
		
		return resource;
	}

	@Override
	public void deleteResource(Resource resource) {
		if (resource != null && resource instanceof FileSystemResource) {
			if (((FileSystemResource) resource).getFile().delete()) {
				logger.debug(String.format("Deleted resource %s", resource));
			}
		}
	}

}
