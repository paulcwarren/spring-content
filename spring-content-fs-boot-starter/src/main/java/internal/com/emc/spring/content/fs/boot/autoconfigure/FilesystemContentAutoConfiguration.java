package internal.com.emc.spring.content.fs.boot.autoconfigure;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

@Configuration
@Import(FilesystemContentAutoConfigureRegistrar.class)
public class FilesystemContentAutoConfiguration {

	@Autowired
	FilesystemContentProperties properties;

	@Bean
	public File fileSystemRoot() throws IOException {
		File fileSystemRoot = null;
		if (this.properties.getFilesystemRoot() != null) {
			fileSystemRoot = new File(this.properties.getFilesystemRoot());
		}
		else {
			fileSystemRoot = File.createTempFile("spring-content", Long.toString(new Date().getTime()));
		}
		fileSystemRoot.mkdirs();
		return fileSystemRoot;
	}

	@Component
	@ConfigurationProperties(prefix = "spring.content", exceptionIfInvalid = true, ignoreUnknownFields = true)
	public static class FilesystemContentProperties {

		String filesystemRoot;

		public String getFilesystemRoot() {
			return this.filesystemRoot;
		}

		public void setFilesystemRoot(String filesystemRoot) {
			this.filesystemRoot = filesystemRoot;
		}
	}
}
