package internal.org.springframework.content.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.annotations.ContentId;
import org.springframework.content.annotations.ContentLength;
import org.springframework.content.common.repository.ContentStore;
import org.springframework.content.common.utils.BeanUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;

import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.util.IOUtils;

public class DefaultS3ContentStoreImpl<S, SID extends Serializable> implements ContentStore<S,SID> {

	private static Log logger = LogFactory.getLog(DefaultS3ContentStoreImpl.class);
	
	private ResourceLoader resourceLoader;
	private AmazonS3 client;
	private String bucket;

	public DefaultS3ContentStoreImpl(ResourceLoader resourceLoader, AmazonS3 client, Region region, String bucket) {
		this.resourceLoader = resourceLoader;
		this.client = client;	
		this.bucket = bucket;
		
        client.setRegion(region);
	}

	@Override
	public void setContent(S property, InputStream content) {
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null) {
			contentId = UUID.randomUUID();
			BeanUtils.setFieldWithAnnotation(property, ContentId.class, contentId.toString());
		}

		Resource resource = resourceLoader.getResource("s3://" + bucket + "/" + contentId);
		OutputStream os = null;
		try {
			if (resource instanceof WritableResource) {
				os = ((WritableResource)resource).getOutputStream();
				IOUtils.copy(content, os);
			}
		} catch (IOException e) {
			logger.error(String.format("Unexpected error setting content %s", contentId.toString()), e);
		} finally {
			IOUtils.closeQuietly(os, null);
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
		
		Resource resource = resourceLoader.getResource("s3://" + bucket + "/" + contentId);
		try {
			if (resource.exists()) {
				return resource.getInputStream();
			}
		} catch (AmazonS3Exception ase) {
			return null;
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
			Resource resource = resourceLoader.getResource("s3://" + bucket + "/" + contentId);
			if (resource.exists()) {
				client.deleteObject(new DeleteObjectRequest(bucket, contentId.toString()));

				// reset content fields
		        BeanUtils.setFieldWithAnnotation(property, ContentId.class, null);
		        BeanUtils.setFieldWithAnnotation(property, ContentLength.class, 0);
			}
		} catch (AmazonS3Exception ase) {
			logger.error(String.format("Unexpected error unsetting content %s", contentId.toString()), ase);
		}
	}
}
