package internal.com.emc.spring.content.fs.repository;

import java.io.File;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.emc.spring.content.commons.repository.AstractResourceContentRepositoryImpl;

public class DefaultFileSystemContentRepositoryImpl<S, SID extends Serializable> extends AstractResourceContentRepositoryImpl<S,SID> {

	private static Log logger = LogFactory.getLog(DefaultFileSystemContentRepositoryImpl.class);
	
	private File fileSystemRoot;
	private ResourceLoader resourceLoader;

	public DefaultFileSystemContentRepositoryImpl(File fileSystemRoot) {
		super(new ContextFileSystemResourceLoader(fileSystemRoot));
		this.fileSystemRoot = fileSystemRoot;
	}

	@Override
	protected String getlocation(Object contentId) {
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
