package internal.org.springframework.content.fs.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.FileService;
import org.springframework.content.fs.io.DeletableResource;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;

public class DefaultFileSystemContentRepositoryImpl<S, SID extends Serializable> implements Store<SID>, ContentStore<S,SID> {

	private static Log logger = LogFactory.getLog(DefaultFileSystemContentRepositoryImpl.class);

	private FileSystemResourceLoader loader;
	private ConversionService conversion;
	private FileService fileService;


	public DefaultFileSystemContentRepositoryImpl(FileSystemResourceLoader loader, ConversionService conversion, FileService fileService) {
		this.loader = loader;
		this.conversion = conversion;
		this.fileService = fileService;
	}

	@Override
	public Resource getResource(SID id) {
		String location = conversion.convert(id, String.class);
		Resource resource = loader.getResource(location);
		return resource;
	}
	
	public void associate(S entity, SID id) {
		BeanUtils.setFieldWithAnnotation(entity, ContentId.class, id.toString());
		String location = conversion.convert(id, String.class);
		Resource resource = loader.getResource(location);
		try {
			BeanUtils.setFieldWithAnnotation(entity, ContentLength.class, resource.contentLength());
		} catch (IOException e) {
			logger.error(String.format("Unexpected error setting content length for %s", id.toString()), e);
		}
	}
	
	public void unassociate(S entity) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setContent(S property, InputStream content) {
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null) {
			contentId = UUID.randomUUID();
			BeanUtils.setFieldWithAnnotation(property, ContentId.class, contentId.toString());
		}

		String location = conversion.convert(contentId, String.class);
		Resource resource = loader.getResource(location);
		OutputStream os = null;
		try {
		    if (resource.exists() == false) {
		        File resourceFile = resource.getFile();
		        File parent = resourceFile.getParentFile();
		        this.fileService.mkdirs(parent);
            }
			if (resource instanceof WritableResource) {
				os = ((WritableResource)resource).getOutputStream();
				IOUtils.copy(content, os);
			}
		} catch (IOException e) {
			logger.error(String.format("Unexpected error setting content %s", contentId.toString()), e);
		} finally {
	        try {
	            if (os != null) {
	                os.close();
	            }
	        } catch (IOException ioe) {
	            // ignore
	        }
		}
			
		try {
			BeanUtils.setFieldWithAnnotation(property, ContentLength.class, resource.contentLength());
		} catch (IOException e) {
			logger.error(String.format("Unexpected error setting content length for content %s", contentId.toString()), e);
		}
	}

	@Override
	public InputStream getContent(S property) {
		if (property == null)
			return null;
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null)
			return null;

		String location = conversion.convert(contentId, String.class);
		Resource resource = loader.getResource(location);
		
		try {
			if (resource.exists()) {
				return resource.getInputStream();
			}
		} catch (IOException e) {
			logger.error(String.format("Unexpected error getting content %s", contentId.toString()), e);
		}
		
		return null;
	}

	@Override
	public void unsetContent(S property) {
		if (property == null)
			return;
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null)
			return;
	
		// delete any existing content object	
		String location = conversion.convert(contentId, String.class);
		Resource resource = loader.getResource(location);

		if (resource.exists() && resource instanceof DeletableResource) {
			((DeletableResource)resource).delete();
		}

		// reset content fields
		BeanUtils.setFieldWithAnnotation(property, ContentId.class, null);
		BeanUtils.setFieldWithAnnotation(property, ContentLength.class, 0);
	}
}
