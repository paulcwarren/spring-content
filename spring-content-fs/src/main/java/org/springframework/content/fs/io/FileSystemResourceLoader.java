package org.springframework.content.fs.io;

import static org.springframework.util.StringUtils.cleanPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

/**
 * {@link ResourceLoader} implementation that resolves plain paths as
 * {@link DeletableResource} file system resources rather than as class path resources
 * (the latter is {@link DefaultResourceLoader}'s default strategy).
 *
 * <p><b>NOTE:</b> Plain paths will always be interpreted relative
 * to the root specified during instantiation rather than relative to the current VM working directory
 * (the latter is {@link org.springframework.core.io.FileSystemResourceLoader}'s default behavior, even if they start with
 * a slash.
 */
public class FileSystemResourceLoader extends org.springframework.core.io.FileSystemResourceLoader {

	private static final Logger logger = LoggerFactory.getLogger(FileSystemResourceLoader.class);

	private FileSystemResource root;
	
	public FileSystemResourceLoader(String root) {
	    Assert.notNull(root);
		logger.info(String.format("Defaulting filesystem root to %s", root));
		this.root = new FileSystemResource(suffixPath(cleanPath(root)));
	}

	public String getFilesystemRoot() {
		return root.getPath();
	}

    private String suffixPath(String path) {
	    if (path.endsWith("/") == false) {
	        return path + "/";
        }
        return path;
    }

    @Override
	public Resource getResource(String location) {
        Assert.notNull(root);
		Resource resource = root.createRelative(location);
		if (resource instanceof FileSystemResource) {
			resource = new FileSystemDeletableResource((FileSystemResource)resource);
		}
		return resource;
	}
}
