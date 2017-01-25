package internal.org.springframework.content.fs.config;

import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.bind.PropertySourcesPropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.core.env.ConfigurableEnvironment;

public class FilesystemProperties implements InitializingBean {

	private static Log logger = LogFactory.getLog(FilesystemProperties.class);
	
	@Autowired 
	private ConfigurableEnvironment env;
	
	private String filesystemRoot;

	public FilesystemProperties() {
	}

	public String getFilesystemRoot() {
		if (filesystemRoot == null) {
			try {
				logger.info(String.format("Defaulting filesystem root to %s", filesystemRoot));
				filesystemRoot = Files.createTempDirectory("").toString();
			} catch (IOException e) {
				logger.error(String.format("Unexpected error defaulting filesystem root to %s", filesystemRoot));
			}
		}
		return filesystemRoot;
	}

	public void setFilesystemRoot(String filesystemRoot) {
		this.filesystemRoot = filesystemRoot;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		new RelaxedDataBinder(this, "spring.content.fs").bind(new PropertySourcesPropertyValues(env.getPropertySources()));		
	}
}
