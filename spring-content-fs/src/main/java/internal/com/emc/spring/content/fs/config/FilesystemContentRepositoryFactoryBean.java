package internal.com.emc.spring.content.fs.config;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.emc.spring.content.commons.repository.factory.AbstractContentStoreFactoryBean;

import internal.com.emc.spring.content.fs.repository.DefaultFileSystemContentRepositoryImpl;

public class FilesystemContentRepositoryFactoryBean extends AbstractContentStoreFactoryBean {

	private static Log logger = LogFactory.getLog(DefaultFileSystemContentRepositoryImpl.class);
	
	@Autowired
	File fileSystemRoot;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
	}

	@Override
	protected Object getContentStoreImpl() {
		logger.info(String.format("File system root set to: %s", fileSystemRoot.toString()));
		return new DefaultFileSystemContentRepositoryImpl(fileSystemRoot);
	}

}
