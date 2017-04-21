package internal.org.springframework.content.fs.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.factory.AbstractContentStoreFactoryBean;
import org.springframework.content.commons.utils.FileServiceImpl;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.Assert;

import internal.org.springframework.content.fs.repository.DefaultFileSystemContentRepositoryImpl;

@SuppressWarnings("rawtypes")
public class FilesystemContentRepositoryFactoryBean extends AbstractContentStoreFactoryBean {

	@Autowired
	FileSystemResourceLoader loader;
	
	@Autowired
	ConversionService filesystemStoreConverter;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		
		Assert.notNull(loader, "resource loader cannot be null");
		Assert.notNull(loader, "filesystemStoreConverter cannot be null");
	}

	@Override
	protected Object getContentStoreImpl() {
		return new DefaultFileSystemContentRepositoryImpl(loader, filesystemStoreConverter, new FileServiceImpl());
	}

}
