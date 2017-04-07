	package internal.org.springframework.content.s3.store;

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
import org.springframework.content.commons.placement.PlacementService;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;

public class DefaultS3StoreImpl<S, SID extends Serializable> implements ContentStore<S,SID> {

	private static Log logger = LogFactory.getLog(DefaultS3StoreImpl.class);

	private ResourceLoader loader;
	private PlacementService placement;
	private AmazonS3 client;
	private String bucket;

	public DefaultS3StoreImpl(ResourceLoader loader, PlacementService placement, AmazonS3 client, String bucket) {
		this.loader = loader;
		this.placement = placement;
		this.client = client;
		this.bucket = bucket;
	}

	@Override
	public void setContent(S property, InputStream content) {
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null) {
			contentId = UUID.randomUUID();
			BeanUtils.setFieldWithAnnotation(property, ContentId.class, contentId.toString());
		}

		String location = placement.getLocation(contentId);
		location = absolutify(location);
		Resource resource = loader.getResource(location);
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

		String location = placement.getLocation(contentId);
		location = absolutify(location);
		Resource resource = loader.getResource(location);
		resource = checkOriginalPlacementStrategy(contentId, resource);
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
			String location = placement.getLocation(contentId);
			location = absolutify(location);
			Resource resource = loader.getResource(location);
			resource = checkOriginalPlacementStrategy(contentId, resource);
			if (resource.exists()) {
				this.delete(resource);
			}

			// reset content fields
	        BeanUtils.setFieldWithAnnotation(property, ContentId.class, null);
	        BeanUtils.setFieldWithAnnotation(property, ContentLength.class, 0);
		} catch (Exception ase) {
			logger.error(String.format("Unexpected error unsetting content %s", contentId.toString()), ase);
		}
	}

	private String absolutify(String location) {
		String locationToUse = null;
		Assert.state(location.startsWith("s3://") == false);
		if (location.startsWith("/")) {
			locationToUse = location.substring(1);
		} else {
			locationToUse = location;
		}
		return String.format("s3://%s/%s", bucket, locationToUse);
	}
	
	/* package */ Resource checkOriginalPlacementStrategy(Object contentId, Resource resource) {
		if (resource.exists() == false) {
			resource = loader.getResource(absolutify(contentId.toString()));
		}
		return resource;
	}

	private void delete(Resource resource) {
		if (resource.exists()) {
			client.deleteObject(new DeleteObjectRequest(bucket, resource.getFilename()));
		}
	}
}
