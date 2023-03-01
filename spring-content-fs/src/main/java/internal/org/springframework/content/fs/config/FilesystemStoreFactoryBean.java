package internal.org.springframework.content.fs.config;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;
import org.springframework.content.commons.utils.FileServiceImpl;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.util.Assert;
import org.springframework.versions.LockingAndVersioningProxyFactory;

import internal.org.springframework.content.fs.repository.DefaultFilesystemStoreImpl;

import java.io.Serializable;

@SuppressWarnings("rawtypes")
public class FilesystemStoreFactoryBean extends AbstractStoreFactoryBean {

	@Autowired
	FileSystemResourceLoader loader;

	@Autowired
	PlacementService filesystemStorePlacementService;

	@Autowired(required=false)
	private LockingAndVersioningProxyFactory versioning;

    @Autowired(required=false)
    private MappingContext mappingContext;

	public FilesystemStoreFactoryBean(Class<? extends Store> storeInterface) {
		super(storeInterface);
	}

	@Override
	protected void addProxyAdvice(ProxyFactory result, BeanFactory beanFactory) {
		if (versioning != null) {
			versioning.apply(result);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();

		Assert.notNull(loader, "resource loader cannot be null");
		Assert.notNull(filesystemStorePlacementService, "filesystemStorePlacementService cannot be null");
	}

	@Override
	protected Object getContentStoreImpl() {
		return new DefaultFilesystemStoreImpl(loader, mappingContext, filesystemStorePlacementService, new FileServiceImpl());
	}
}
