package org.springframework.content.commons.operations;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;

public abstract class AbstractResourceTemplate implements ResourceOperations, ContentOperations {
	
	private static Log logger = LogFactory.getLog(AbstractResourceTemplate.class);
	
	private ResourceLoader resourceLoader;
	
	public AbstractResourceTemplate(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}
	
	@Override
	public <S> void setContent(S property, InputStream content) {
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null) {
			contentId = UUID.randomUUID();
			BeanUtils.setFieldWithAnnotation(property, ContentId.class, contentId.toString());
		}

		Resource resource = this.get(this.getlocation(contentId));
		OutputStream os = null;
		try {
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
	public <S> InputStream getContent(S property) {
		if (property == null)
			return null;
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null)
			return null;

		Resource resource = this.get(this.getlocation(contentId));
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
	public <S> void unsetContent(S property) {
		if (property == null)
			return;
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null)
			return;

		// delete any existing content object
		try {
			Resource resource = this.get(this.getlocation(contentId));
			if (resource.exists()) {
				this.delete(resource);

				// reset content fields
		        BeanUtils.setFieldWithAnnotation(property, ContentId.class, null);
		        BeanUtils.setFieldWithAnnotation(property, ContentLength.class, 0);
			}
		} catch (Exception ase) {
			logger.error(String.format("Unexpected error unsetting content %s", contentId.toString()), ase);
		}
	}

    @Override
	public Resource get(String location) {
		Resource resource = resourceLoader.getResource(location);
		return resource;
	}

	@Override
	public void delete(Resource resource) {
		try {
			if (resource.exists()) {
				this.deleteResource(resource);
			}
		} catch (Exception ase) {
			logger.error(String.format("Unexpected error unsetting content %s", resource.getFilename()), ase);
		}
	}


	// hook for our placement strategy
	protected abstract String getlocation(Object contentId);


	// todo: remove this
	protected abstract void deleteResource(Resource resource) throws Exception;

}
