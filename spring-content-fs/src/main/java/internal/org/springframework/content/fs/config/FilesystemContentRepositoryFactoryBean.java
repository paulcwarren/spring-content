package internal.org.springframework.content.fs.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.placementstrategy.PlacementService;
import org.springframework.content.commons.repository.factory.AbstractContentStoreFactoryBean;
import org.springframework.content.commons.utils.FileServiceImpl;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.util.Assert;

import internal.org.springframework.content.fs.repository.DefaultFileSystemContentRepositoryImpl;

@SuppressWarnings("rawtypes")
public class FilesystemContentRepositoryFactoryBean extends AbstractContentStoreFactoryBean {

	@Autowired
	FileSystemResourceLoader loader;
	
	@Autowired
	PlacementService placement;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		
		Assert.notNull(loader, "resource loader cannot be null");
		Assert.notNull(placement, "placement service cannot be null");
	}

	@Override
	protected Object getContentStoreImpl() {
		return new DefaultFileSystemContentRepositoryImpl(loader, placement, new FileServiceImpl());
	}

}
