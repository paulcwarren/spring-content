package internal.org.springframework.content.jpa.config;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.common.repository.factory.AbstractContentStoreFactoryBean;

import internal.org.springframework.content.jpa.repository.DefaultFileSystemContentRepositoryImpl;

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
