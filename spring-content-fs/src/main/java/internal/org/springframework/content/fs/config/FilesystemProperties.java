package internal.org.springframework.content.fs.config;

import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;

public class FilesystemProperties {

	private static Log logger = LogFactory.getLog(FilesystemProperties.class);
	
	private String filesystemRoot;

	public FilesystemProperties() {
	}

	public String getFilesystemRoot() {
		if (filesystemRoot == null) {
			try {
				filesystemRoot = Files.createTempDirectory("").toString();
				logger.info(String.format("Defaulting filesystem root to %s", filesystemRoot));
			} catch (IOException e) {
				logger.error(String.format("Unexpected error defaulting filesystem root to %s", filesystemRoot));
			}
		}
		return filesystemRoot;
	}

	@Value("${spring.content.fs.filesystemRoot:#{null}}")
	public void setFilesystemRoot(String filesystemRoot) {
		this.filesystemRoot = filesystemRoot;
	}
}
