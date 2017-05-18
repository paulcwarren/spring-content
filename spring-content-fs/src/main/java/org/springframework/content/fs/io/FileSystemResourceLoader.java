package org.springframework.content.fs.io;

import static org.springframework.util.StringUtils.cleanPath;

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

	private FileSystemResource root;
	
	public FileSystemResourceLoader(String root) {
	    Assert.notNull(root);
		this.root = new FileSystemResource(suffixPath(cleanPath(root)));
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
