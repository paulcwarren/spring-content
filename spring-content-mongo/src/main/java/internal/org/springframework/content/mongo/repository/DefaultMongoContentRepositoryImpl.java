package internal.org.springframework.content.mongo.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.operations.AbstractResourceTemplate;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import internal.org.springframework.content.mongo.operations.MongoContentTemplate;

public class DefaultMongoContentRepositoryImpl<S, SID extends Serializable> implements ContentStore<S,SID> {

	private static Log logger = LogFactory.getLog(AbstractResourceTemplate.class);
	
	private MongoContentTemplate template;

	public DefaultMongoContentRepositoryImpl(MongoContentTemplate template) {
		this.template = template;

		Assert.notNull(this.template, "MongoContentTemplate cannot be null");
	}

	@Override
	public void setContent(S property, InputStream content) {
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null) {
			contentId = UUID.randomUUID();
			BeanUtils.setFieldWithAnnotation(property, ContentId.class, contentId.toString());
		}

		Resource resource = template.get(template.getLocation(contentId));
		if (resource != null) {
			template.delete(resource);
		}

		resource = template.create(template.getLocation(contentId), content);

		long contentLen = 0L;
		try {
			contentLen = resource.contentLength();
		} catch (IOException ioe) {
			logger.debug(String.format("Unable to retrieve content length for %s", contentId));
		}
		BeanUtils.setFieldWithAnnotation(property, ContentLength.class, contentLen);
	}

	@Override
	public InputStream getContent(S property) {
		if (property == null)
			return null;
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null)
			return null;

		Resource resource = template.get(template.getLocation(contentId));
		try {
			if (resource != null && resource.exists()) {
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
			Resource resource = template.get(template.getLocation(contentId));
			if (resource != null && resource.exists()) {
				template.delete(resource);

				// reset content fields
				BeanUtils.setFieldWithAnnotation(property, ContentId.class, null);
				BeanUtils.setFieldWithAnnotation(property, ContentLength.class, 0);
			}
		} catch (Exception ase) {
			logger.error(String.format("Unexpected error unsetting content %s", contentId.toString()), ase);
		}

		this.template.unsetContent(property);
	}
}
