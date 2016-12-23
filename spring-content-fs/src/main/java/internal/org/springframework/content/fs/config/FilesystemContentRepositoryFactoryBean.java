package internal.org.springframework.content.fs.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.factory.AbstractContentStoreFactoryBean;
import org.springframework.util.Assert;

import internal.org.springframework.content.fs.operations.FileResourceTemplate;
import internal.org.springframework.content.fs.repository.DefaultFileSystemContentRepositoryImpl;

public class FilesystemContentRepositoryFactoryBean extends AbstractContentStoreFactoryBean {

	private static Log logger = LogFactory.getLog(DefaultFileSystemContentRepositoryImpl.class);
	
	@Autowired
	FileResourceTemplate template;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		
		Assert.notNull(template, "template cannot be null");
	}

	@Override
	protected Object getContentStoreImpl() {
		return new DefaultFileSystemContentRepositoryImpl(template);
	}

}
