package internal.org.springframework.content.fs.repository;

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
import org.springframework.content.commons.placementstrategy.PlacementService;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;

import internal.org.springframework.content.fs.operations.FileResourceTemplate;

public class DefaultFileSystemContentRepositoryImpl<S, SID extends Serializable> /*extends AstractResourceContentRepositoryImpl<S,SID>*/  implements ContentStore<S,SID> {

	private static Log logger = LogFactory.getLog(DefaultFileSystemContentRepositoryImpl.class);
	
	private FileResourceTemplate template;
	private PlacementService placement;

	public DefaultFileSystemContentRepositoryImpl(FileResourceTemplate template, PlacementService placement) {
		this.template = template;
		this.placement = placement;
	}

	@Override
	public void setContent(S property, InputStream content) {
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null) {
			contentId = UUID.randomUUID();
			BeanUtils.setFieldWithAnnotation(property, ContentId.class, contentId.toString());
		}

		Resource resource = this.template.get(this.template.getLocation(contentId));
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
	public InputStream getContent(S property) {
		if (property == null)
			return null;
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null)
			return null;

		Resource resource = this.template.get(this.template.getLocation(contentId));
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
		try {
			Resource resource = this.template.get(this.template.getLocation(contentId));
			if (resource.exists()) {
				this.template.delete(resource);
	
				// reset content fields
		        BeanUtils.setFieldWithAnnotation(property, ContentId.class, null);
		        BeanUtils.setFieldWithAnnotation(property, ContentLength.class, 0);
			}
		} catch (Exception ase) {
			logger.error(String.format("Unexpected error unsetting content %s", contentId.toString()), ase);
		}
	}
}
