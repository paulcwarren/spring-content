package internal.org.springframework.content.fs.config;

import internal.org.springframework.content.fs.operations.FileResourceTemplate;
import internal.org.springframework.content.fs.repository.DefaultFileSystemContentRepositoryImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.factory.AbstractContentStoreFactoryBean;
import org.springframework.util.Assert;

import java.beans.Introspector;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FilesystemContentRepositoryFactoryBean extends AbstractContentStoreFactoryBean {

	private static Log logger = LogFactory.getLog(DefaultFileSystemContentRepositoryImpl.class);

	@Autowired
	private FilesystemProperties filesystemProperties;

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.notNull(filesystemProperties, "filesystemProperties cannot be null");
	}

	@Override
	protected Object getContentStoreImpl() {
		// TODO ability to change store name (annotation/application properties)?
		String repositoryRoot = Introspector.decapitalize(getObjectType().getSimpleName());
		Path repositoryRootPath = Paths.get(filesystemProperties.getFilesystemRoot(), repositoryRoot);
		// TODO create store root path here?
		if (!repositoryRootPath.toFile().exists())
			repositoryRootPath.toFile().mkdirs();
		return new DefaultFileSystemContentRepositoryImpl(new FileResourceTemplate(repositoryRootPath));
	}

}
