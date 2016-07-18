package internal.com.emc.spring.content.fs.config;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;

import com.emc.spring.content.commons.repository.factory.AbstractContentStoreFactoryBean;

import internal.com.emc.spring.content.fs.repository.DefaultFileSystemContentRepositoryImpl;

public class FilesystemContentRepositoryFactoryBean extends AbstractContentStoreFactoryBean {

	@Autowired
	File fileSystemRoot;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
	}

	@Override
	protected Object getContentStoreImpl() {
		return new DefaultFileSystemContentRepositoryImpl(fileSystemRoot);
	}

}
