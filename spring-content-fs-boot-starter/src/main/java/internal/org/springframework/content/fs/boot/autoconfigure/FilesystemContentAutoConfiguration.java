package internal.org.springframework.content.fs.boot.autoconfigure;

import java.io.File;
import java.io.IOException;

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
			if (fileSystemRoot.exists()) {
				return fileSystemRoot;
			}
			else {
				if (fileSystemRoot.mkdirs()) {
					return fileSystemRoot;
				}
			}
		}
		else {
			File baseDir = new File(System.getProperty("java.io.tmpdir"));
			String baseName = "spring-content-" + System.currentTimeMillis() + "-";

			for (int counter = 0; counter < Integer.MAX_VALUE; counter++) {
				fileSystemRoot = new File(baseDir, baseName + counter);
				if (fileSystemRoot.mkdir()) {
					return fileSystemRoot;
				}
			}
		}
		throw new IllegalStateException(String.format("Failed to create directory filesystem root for Spring Content %s", fileSystemRoot.toString()));
	}

	@Component
	@ConfigurationProperties(prefix = "spring.content.fs", exceptionIfInvalid = true, ignoreUnknownFields = true)
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
