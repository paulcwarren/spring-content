package internal.org.springframework.content.fs.operations;

import internal.org.springframework.content.fs.repository.ContextFileSystemResourceLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.operations.AbstractResourceTemplate;
import org.springframework.content.commons.operations.ContentOperations;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.nio.file.Path;

/**
 * {@link ContentOperations} implementation to store and retrieve file-based content.
 */
public class FileResourceTemplate extends AbstractResourceTemplate implements ContentOperations {

	private static Log logger = LogFactory.getLog(FileResourceTemplate.class);

	private File repositoryRoot;

	public FileResourceTemplate(Path repositoryRootPath) {
		super(new ContextFileSystemResourceLoader(repositoryRootPath.toString()));
		this.repositoryRoot = repositoryRootPath.toFile();
	}

	@Override
	protected String getLocation(Object contentId) {
		return new File(repositoryRoot, contentId.toString()).toURI().toString();
	}

	@Override
	protected void deleteResource(Resource resource) throws Exception {
		if (resource != null && resource instanceof FileSystemResource) {
			if (((FileSystemResource) resource).getFile().delete()) {
				logger.debug(String.format("Deleted resource %s", resource.getFile().getPath()));
			}
		}
	}
}
