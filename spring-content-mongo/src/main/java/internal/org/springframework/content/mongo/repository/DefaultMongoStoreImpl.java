package internal.org.springframework.content.mongo.repository;

import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereFilename;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.Condition;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.util.Assert;

public class DefaultMongoStoreImpl<S, SID extends Serializable> implements ContentStore<S,SID> {

	private static Log logger = LogFactory.getLog(DefaultMongoStoreImpl.class);

	private GridFsTemplate gridFs;
	private ConversionService converter;

	public DefaultMongoStoreImpl(GridFsTemplate gridFs, ConversionService converter) {
		Assert.notNull(gridFs, "gridFs cannot be null");
		Assert.notNull(converter, "converter cannot be null");

		this.gridFs = gridFs;
		this.converter = converter;
	}

	@Override
	public void setContent(S property, InputStream content) {
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null) {
			contentId = UUID.randomUUID();
			BeanUtils.setFieldWithAnnotation(property, ContentId.class, contentId.toString());
		}

		String location = converter.convert(contentId, String.class);
		Resource resource = gridFs.getResource(location);
		if (resource != null && resource.exists()) {
			gridFs.delete(query(whereFilename().is(resource.getFilename())));
		}

		gridFs.store(content, location);
		resource = gridFs.getResource(location);

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

		String location = converter.convert(contentId, String.class);
		Resource resource = gridFs.getResource(location);
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

		try {
			String location = converter.convert(contentId, String.class);
			Resource resource = gridFs.getResource(location);
			if (resource != null && resource.exists()) {
				gridFs.delete(query(whereFilename().is(resource.getFilename())));

				// reset content fields
				BeanUtils.setFieldWithAnnotationConditionally(property, ContentId.class, null, new Condition() {
					@Override
					public boolean matches(Field field) {
						for (Annotation annotation : field.getAnnotations()) {
							if ("javax.persistence.Id".equals(annotation.annotationType().getCanonicalName()) ||
								"org.springframework.data.annotation.Id".equals(annotation.annotationType().getCanonicalName())) {
								return false;
							}
						}
						return true;
					}});
				BeanUtils.setFieldWithAnnotation(property, ContentLength.class, 0);
			}
		} catch (Exception ase) {
			logger.error(String.format("Unexpected error unsetting content %s", contentId.toString()), ase);
		}
	}
}
