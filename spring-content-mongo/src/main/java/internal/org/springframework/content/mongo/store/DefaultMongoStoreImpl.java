package internal.org.springframework.content.mongo.store;

import internal.org.springframework.content.mongo.io.GridFsStoreResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.Condition;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.UUID;

import static java.lang.String.format;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereFilename;

public class DefaultMongoStoreImpl<S, SID extends Serializable>
		implements Store<SID>, AssociativeStore<S, SID>, ContentStore<S, SID> {

	private static Log logger = LogFactory.getLog(DefaultMongoStoreImpl.class);

	private GridFsTemplate gridFs;
	private PlacementService placer;

	public DefaultMongoStoreImpl(GridFsTemplate gridFs, PlacementService placer) {
		Assert.notNull(gridFs, "gridFs cannot be null");
		Assert.notNull(placer, "placer cannot be null");

		this.gridFs = gridFs;
		this.placer = placer;
	}

	@Override
	public Resource getResource(SID id) {
		String location = placer.convert(id, String.class);
		return new GridFsStoreResource(location, gridFs);
	}

	@Override
	public Resource getResource(S entity) {
		if (entity == null)
			return null;

		Object contentId = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
		if (contentId == null) {
			return null;
		}

		String location = placer.convert(contentId, String.class);
		return new GridFsStoreResource(location, gridFs);
	}

	@Override
	public void associate(S entity, SID id) {
		Resource resource = this.getResource(id);
		Object convertedId = convertToExternalContentIdType(entity, id);
		BeanUtils.setFieldWithAnnotation(entity, ContentId.class,
				convertedId.toString());
	}

	@Override
	public void unassociate(S entity) {
		BeanUtils.setFieldWithAnnotationConditionally(entity, ContentId.class, null,
				new Condition() {
					@Override
					public boolean matches(Field field) {
						for (Annotation annotation : field.getAnnotations()) {
							if ("javax.persistence.Id".equals(
									annotation.annotationType().getCanonicalName())
									|| "org.springframework.data.annotation.Id"
											.equals(annotation.annotationType()
													.getCanonicalName())) {
								return false;
							}
						}
						return true;
					}
				});
	}

	@Override
	public S setContent(S property, InputStream content) {
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null) {
			contentId = UUID.randomUUID();
			BeanUtils.setFieldWithAnnotation(property, ContentId.class,
					contentId.toString());
		}

		String location = placer.convert(contentId, String.class);
		Resource resource = gridFs.getResource(location);
		if (resource != null && resource.exists()) {
			gridFs.delete(query(whereFilename().is(resource.getFilename())));
		}

		try {
			gridFs.store(content, location);
			resource = gridFs.getResource(location);
		} catch (Exception e) {
			logger.error(format("Unexpected error setting content for entity  %s", property), e);
			throw new StoreAccessException(format("Setting content for entity %s", property), e);
		}

		long contentLen = 0L;
		try {
			contentLen = resource.contentLength();
		}
		catch (IOException ioe) {
			logger.debug(format("Unable to retrieve content length for %s", contentId));
		}
		BeanUtils.setFieldWithAnnotation(property, ContentLength.class, contentLen);

		return property;
	}

	@Override
	public S setContent(S property, Resource resourceContent) {
		try {
			return setContent(property, resourceContent.getInputStream());
		} catch (IOException e) {
			logger.error(format("Unexpected error setting content for entity  %s", property), e);
			throw new StoreAccessException(format("Setting content for entity %s", property), e);
		}
	}

	@Override
	public InputStream getContent(S entity) {
		if (entity == null)
			return null;
		Object contentId = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
		if (contentId == null)
			return null;

		String location = placer.convert(contentId, String.class);
		Resource resource = gridFs.getResource(location);
		try {
			if (resource != null && resource.exists()) {
				return resource.getInputStream();
			}
		}
		catch (IOException e) {
			logger.error(format("Unexpected error getting content for entityt %s", entity), e);
			throw new StoreAccessException(format("Getting content for entity %s", entity), e);
		}
		return null;
	}

	@Override
	public S unsetContent(S property) {
		if (property == null)
			return property;
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null)
			return property;

		try {
			String location = placer.convert(contentId, String.class);
			Resource resource = gridFs.getResource(location);
			if (resource != null && resource.exists()) {
				gridFs.delete(query(whereFilename().is(resource.getFilename())));

				// reset content fields
				BeanUtils.setFieldWithAnnotationConditionally(property, ContentId.class,
						null, new Condition() {
							@Override
							public boolean matches(Field field) {
								for (Annotation annotation : field.getAnnotations()) {
									if ("javax.persistence.Id"
											.equals(annotation.annotationType()
													.getCanonicalName())
											|| "org.springframework.data.annotation.Id"
													.equals(annotation.annotationType()
															.getCanonicalName())) {
										return false;
									}
								}
								return true;
							}
						});
				BeanUtils.setFieldWithAnnotation(property, ContentLength.class, 0);
			}
		}
		catch (Exception ase) {
			logger.error(format("Unexpected error unsetting content for entity %s", property), ase);
			throw new StoreAccessException(format("Unsetting content for entity %s", property), ase);
		}

		return property;
	}

	protected Object convertToExternalContentIdType(S property, Object contentId) {
		if (placer.canConvert(TypeDescriptor.forObject(contentId),
				TypeDescriptor.valueOf(BeanUtils.getFieldWithAnnotationType(property,
						ContentId.class)))) {
			contentId = placer.convert(contentId, TypeDescriptor.forObject(contentId),
					TypeDescriptor.valueOf(BeanUtils.getFieldWithAnnotationType(property,
							ContentId.class)));
			return contentId;
		}
		return contentId.toString();
	}
}
