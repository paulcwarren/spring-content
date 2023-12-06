package org.springframework.content.fs.io;

import static org.springframework.util.StringUtils.cleanPath;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.utils.FileService;
import org.springframework.content.commons.utils.FileServiceImpl;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

import internal.org.springframework.content.fs.io.FileSystemDeletableResource;

/**
 * {@link ResourceLoader} implementation that resolves plain paths as
 * {@link DeletableResource} file system resources rather than as class path resources
 * (the latter is {@link DefaultResourceLoader}'s default strategy).
 *
 * <p>
 * <b>NOTE:</b> Plain paths will always be interpreted relative to the root specified
 * during instantiation rather than relative to the current VM working directory (the
 * latter is {@link org.springframework.core.io.FileSystemResourceLoader}'s default
 * behavior, even if they start with a slash.
 */
public class FileSystemResourceLoader
		extends org.springframework.core.io.FileSystemResourceLoader {

    private static Log logger = LogFactory.getLog(FileSystemResourceLoader.class);

	private FileSystemResource root;
	private FileService fileService = null;

	public FileSystemResourceLoader(String root) {
		Assert.notNull(root, "root must not be null");
		logger.info(String.format("Defaulting filesystem root to '%s'", root));
		this.root = new FileSystemResource(suffixPath(cleanPath(root)));
		this.fileService = new FileServiceImpl();
	}

	@Deprecated
	public String getFilesystemRoot() {
		return root.getPath();
	}

	public FileSystemResource getRootResource() {
		return root;
	}

	private String suffixPath(String path) {
		if (path.endsWith("/") == false) {
			return path + "/";
		}
		return path;
	}

	@Override
	public Resource getResource(String location) {
		Assert.notNull(root, "root must not be null");
		Resource resource = root.createRelative(location);
		if (resource instanceof FileSystemResource) {
			resource = new FileSystemDeletableResource((FileSystemResource) resource, fileService);
		}
		return resource;
	}
}
