package internal.org.springframework.content.mongo.store;

import static java.lang.String.format;
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
import org.bson.types.ObjectId;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.SetContentParams;
import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.Condition;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import internal.org.springframework.content.mongo.io.GridFsStoreResource;

public class DefaultMongoStoreImpl<S, SID extends Serializable>
		implements org.springframework.content.commons.repository.Store<SID>,
        org.springframework.content.commons.repository.AssociativeStore<S, SID>,
        org.springframework.content.commons.repository.ContentStore<S, SID>,
        AssociativeStore<S, SID> {

	private static Log logger = LogFactory.getLog(DefaultMongoStoreImpl.class);

	private GridFsTemplate gridFs;
	private PlacementService placer;

    private MappingContext mappingContext;

	public DefaultMongoStoreImpl(GridFsTemplate gridFs, MappingContext mappingContext, PlacementService placer) {
		Assert.notNull(gridFs, "gridFs cannot be null");
		Assert.notNull(placer, "placer cannot be null");

		this.gridFs = gridFs;
		this.placer = placer;

		this.mappingContext = mappingContext;
        this.mappingContext = mappingContext;
        if (this.mappingContext == null) {
            this.mappingContext = new MappingContext("/", ".");
        }
	}

	@Override
	public Resource getResource(SID id) {
	    if (id == null) {
	        return null;
	    }

	    String location = placer.convert(id, String.class);
		return new GridFsStoreResource(location, gridFs);
	}

	@Override
	public Resource getResource(S entity) {
		if (entity == null)
			return null;

		ObjectId objectId = null;
		if (placer.canConvert(entity.getClass(), ObjectId.class)) {
		    objectId = placer.convert(entity, ObjectId.class);

		    if (objectId != null) {
		        String location = placer.convert(objectId, String.class);
		        return new GridFsStoreResource(location, gridFs);
		    }
		}

		SID contentId = (SID) BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
		return this.getResource(contentId);
	}

    @Override
    public Resource getResource(S entity, PropertyPath propertyPath) {
        return this.getResource(entity, propertyPath, GetResourceParams.builder().build());
    }

    @Override
    public Resource getResource(S entity, PropertyPath propertyPath, org.springframework.content.commons.repository.GetResourceParams params) {
        return this.getResource(entity, propertyPath, GetResourceParams.builder().range(params.getRange()).build());
    }

    @Override
    public Resource getResource(S entity, PropertyPath propertyPath, GetResourceParams params) {
        if (entity == null)
            return null;

        ObjectId objectId = null;
        if (placer.canConvert(entity.getClass(), ObjectId.class)) {
            objectId = placer.convert(entity, ObjectId.class);

            if (objectId != null) {
                String location = placer.convert(objectId, String.class);
                return new GridFsStoreResource(location, gridFs);
            }
        }

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        SID contentId = (SID) property.getContentId(entity);

        return this.getResource(contentId);
    }

    @Override
	public void associate(S entity, SID id) {
		Resource resource = this.getResource(id);
		Object convertedId = convertToExternalContentIdType(entity, id);
		BeanUtils.setFieldWithAnnotation(entity, ContentId.class,
				convertedId.toString());
	}

    @Override
    public void associate(S entity, PropertyPath propertyPath, SID id) {

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        Resource resource = this.getResource(id);

        Object convertedId = convertToExternalContentIdType(id, property.getContentIdType(entity));
        property.setContentId(entity, convertedId, null);
    }

	@Override
	public void unassociate(S entity) {
		BeanUtils.setFieldWithAnnotationConditionally(entity, ContentId.class, null,
				new Condition() {
					@Override
					public boolean matches(Field field) {
						for (Annotation annotation : field.getAnnotations()) {
							if ("jakarta.persistence.Id".equals(
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
    public void unassociate(S entity, PropertyPath propertyPath) {

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        property.setContentId(entity, null, new org.springframework.content.commons.mappingcontext.Condition() {
            @Override
            public boolean matches(TypeDescriptor descriptor) {
                for (Annotation annotation : descriptor.getAnnotations()) {
                    if ("jakarta.persistence.Id".equals(
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
    @Transactional
	public S setContent(S entity, InputStream content) {

        Object contentId = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (contentId == null) {

            Serializable newId = UUID.randomUUID().toString();

            Object convertedId = convertToExternalContentIdType(entity, newId);

            BeanUtils.setFieldWithAnnotation(entity, ContentId.class, convertedId);
        }

		Resource resource = this.getResource(entity);
        if (resource == null) {
            return entity;
        }

		if (resource.exists()) {
			gridFs.delete(query(whereFilename().is(resource.getFilename())));
		}

		try {
			gridFs.store(content, resource.getFilename());
			resource = gridFs.getResource(resource.getFilename());
		} catch (Exception e) {
			logger.error(format("Unexpected error setting content for entity  %s", entity), e);
			throw new StoreAccessException(format("Setting content for entity %s", entity), e);
		}

		long contentLen = 0L;
		try {
			contentLen = resource.contentLength();
		}
		catch (IOException ioe) {
			logger.debug(format("Unable to retrieve content length for %s", contentId));
		}
		BeanUtils.setFieldWithAnnotation(entity, ContentLength.class, contentLen);

		return entity;
	}

	@Transactional
    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream content) {
        return this.setContent(entity, propertyPath, content, -1L);
    }

    @Transactional
    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream content, long contentLen) {

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        Object contentId = property.getContentId(entity);
        if (contentId == null) {

            Serializable newId = UUID.randomUUID().toString();

            Object convertedId = convertToExternalContentIdType(newId, property.getContentIdType(entity));

            property.setContentId(entity, convertedId, null);
        }

        Resource resource = this.getResource(entity, propertyPath);
        if (resource == null) {
            return entity;
        }

        if (resource.exists()) {
            gridFs.delete(query(whereFilename().is(resource.getFilename())));
        }

        try {
            gridFs.store(content, resource.getFilename());
            resource = gridFs.getResource(resource.getFilename());
        } catch (Exception e) {
            logger.error(format("Unexpected error setting content for entity  %s", entity), e);
            throw new StoreAccessException(format("Setting content for entity %s", entity), e);
        }

        try {
            long len = contentLen;
            if (len == -1L) {
                len = resource.contentLength();
            }
            property.setContentLength(entity, len);
        }
        catch (IOException ioe) {
            logger.debug(format("Unable to retrieve content length for %s", contentId));
        }

        return entity;
    }

    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream content, SetContentParams params) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Transactional
	public S setContent(S property, Resource resourceContent) {
		try {
			return setContent(property, resourceContent.getInputStream());
		} catch (IOException e) {
			logger.error(format("Unexpected error setting content for entity  %s", property), e);
			throw new StoreAccessException(format("Setting content for entity %s", property), e);
		}
	}

    @Transactional
    @Override
    public S setContent(S entity, PropertyPath propertyPath, Resource resourceContent) {
        try {
            return setContent(entity, propertyPath, resourceContent.getInputStream());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity  %s", entity), e);
            throw new StoreAccessException(format("Setting content for entity %s", entity), e);
        }
    }

	@Override
    @Transactional
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

    @Transactional
    @Override
    public InputStream getContent(S entity, PropertyPath propertyPath) {

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        if (entity == null)
            return null;

        Object contentId = property.getContentId(entity);
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
    @Transactional
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
									if ("jakarta.persistence.Id"
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

    @Override
    public S unsetContent(S entity, PropertyPath propertyPath) {

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        if (entity == null)
            return entity;

        Object contentId = property.getContentId(entity);
        if (contentId == null)
            return entity;

        try {
            String location = placer.convert(contentId, String.class);
            Resource resource = gridFs.getResource(location);
            if (resource != null && resource.exists()) {
                gridFs.delete(query(whereFilename().is(resource.getFilename())));

                // reset content fields
                property.setContentId(entity, null, new org.springframework.content.commons.mappingcontext.Condition() {
                    @Override
                    public boolean matches(TypeDescriptor descriptor) {
                        for (Annotation annotation : descriptor.getAnnotations()) {
                            if ("jakarta.persistence.Id".equals(
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

                property.setContentLength(entity, 0);
            }
        }
        catch (Exception ase) {
            logger.error(format("Unexpected error unsetting content for entity %s", entity), ase);
            throw new StoreAccessException(format("Unsetting content for entity %s", entity), ase);
        }

        return entity;
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

    private Object convertToExternalContentIdType(Object contentId, TypeDescriptor contentIdType) {
        if (placer.canConvert(TypeDescriptor.forObject(contentId),
                contentIdType)) {
            contentId = placer.convert(contentId, TypeDescriptor.forObject(contentId),
                    contentIdType);
            return contentId;
        }
        return contentId.toString();
    }
}
