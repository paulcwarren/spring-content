package internal.org.springframework.content.rest.links;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import internal.org.springframework.content.rest.utils.ContentStoreUtils;
import internal.org.springframework.content.rest.utils.DomainObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.RepositoryResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.BaseUri;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.support.RepositoryLinkBuilder;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkBuilder;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.mvc.BasicLinkBuilder;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Adds content and content collection links to Spring Data REST Entity Resources.
 * 
 * @author warrep
 *
 */
public class ContentLinksResourceProcessor
		implements ResourceProcessor<PersistentEntityResource> {

	private static final Log log = LogFactory.getLog(ContentLinksResourceProcessor.class);

	private ContentStoreService stores;
	private RestConfiguration config;
	private RepositoryResourceMappings mappings;

	public ContentLinksResourceProcessor(Repositories repos, ContentStoreService stores, RestConfiguration config, RepositoryResourceMappings mappings) {
		this.stores = stores;
		this.config = config;
		this.mappings = mappings;
	}

	public PersistentEntityResource process(final PersistentEntityResource resource) {

		Object object = resource.getContent();
		if (object == null)
			return resource;

		Object entityId = DomainObjectUtils.getId(object);

		ContentStoreInfo store = ContentStoreUtils.findContentStore(stores, object.getClass());

		Field[] fields = BeanUtils.findFieldsWithAnnotation(object.getClass(), ContentId.class, new BeanWrapperImpl(object));
		if (fields.length == 1) {
			if (store != null) {
				resource.add(shortcutLink(config.getBaseUri(), store, entityId));
			}
		} else if (fields.length > 1) {
			for (Field field : fields) {
				resource.add(fullyQualifiedLink(config.getBaseUri(), store, entityId, field.getName()));
			}
		}

		List<Field> processed = new ArrayList<>();

		ResourceMetadata md = mappings.getMetadataFor(object.getClass());

		// public fields
		for (Field field : object.getClass().getFields()) {
			processed.add(field);
			handleField(field, resource, md, config.getBaseUri(), entityId);
		}

		// handle properties
		BeanWrapper wrapper = new BeanWrapperImpl(object);
		for (PropertyDescriptor descriptor : wrapper.getPropertyDescriptors()) {
			Field field = null;
			try {
				field = object.getClass().getDeclaredField(descriptor.getName());
				if (processed.contains(field) == false) {
					handleField(field, resource, md, config.getBaseUri(), entityId);
				}
			}
			catch (NoSuchFieldException nsfe) {
				log.trace(String.format("No field for property %s, ignoring",
						descriptor.getName()));
			}
			catch (SecurityException se) {
				log.warn(String.format(
						"Unexpected security error while handling content links for property %s",
						descriptor.getName()));
			}
		}

		return resource;
	}

	private void handleField(Field field, final PersistentEntityResource resource, ResourceMetadata metadata, URI baseUri, Object entityId) {

		Class<?> fieldType = field.getType();
		if (fieldType.isArray()) {
			fieldType = fieldType.getComponentType();

			ContentStoreInfo store = ContentStoreUtils.findContentStore(stores, fieldType);
			if (store != null) {
				resource.add(propertyLink(metadata, baseUri, entityId, field.getName(), null));
			}
		}
		else if (Collection.class.isAssignableFrom(fieldType)) {
			Type type = field.getGenericType();

			if (type instanceof ParameterizedType) {

				ParameterizedType pType = (ParameterizedType) type;
				Type[] arr = pType.getActualTypeArguments();

				for (Type tp : arr) {
					fieldType = (Class<?>) tp;
				}

				ContentStoreInfo store = ContentStoreUtils.findContentStore(stores,
						fieldType);
				if (store != null) {
					Object object = resource.getContent();
					BeanWrapper wrapper = new BeanWrapperImpl(object);
					Object value = null;
					try {
						value = wrapper.getPropertyValue(field.getName());
					}
					catch (InvalidPropertyException ipe) {
						try {
							value = ReflectionUtils.getField(field, object);
						}
						catch (IllegalStateException ise) {
							log.trace(String.format("Didn't get value for property %s", field.getName()));
						}
					}
					if (value != null) {
						Iterator iter = ((Collection) value).iterator();
						while (iter.hasNext()) {
							Object o = iter.next();
							if (BeanUtils.hasFieldWithAnnotation(o, ContentId.class)) {
								String cid = BeanUtils.getFieldWithAnnotation(o, ContentId.class).toString();
								if (cid != null) {
									resource.add(propertyLink(metadata, baseUri, entityId, field.getName(), cid));
								}
							}
						}
					}
				}
			}
		}
		else {
			ContentStoreInfo store = ContentStoreUtils.findContentStore(stores,
					fieldType);
			if (store != null) {
				Object object = resource.getContent();
				BeanWrapper wrapper = new BeanWrapperImpl(object);
				Object value = null;
				try {
					value = wrapper.getPropertyValue(field.getName());
				}
				catch (InvalidPropertyException ipe) {
					try {
						value = ReflectionUtils.getField(field, object);
					}
					catch (IllegalStateException ise) {
						log.trace(String.format("Didn't get value for property %s", field.getName()));
					}
				}
				if (value != null) {
					String cid = BeanUtils.getFieldWithAnnotation(value, ContentId.class).toString();
					if (cid != null) {
						resource.add(propertyLink(metadata, baseUri, entityId, field.getName(), cid));
					}
				}
			}
		}
	}

	private Link shortcutLink(URI baseUri, ContentStoreInfo store, Object id) {
		LinkBuilder builder = BasicLinkBuilder.linkToCurrentMapping();

		if (baseUri != null) {
			builder = builder.slash(baseUri);
		}

		return builder.slash(ContentStoreUtils.storePath(store))
				.slash(id)
				.withRel(StringUtils.uncapitalize(ContentStoreUtils.getSimpleName(store)));
	}

	private Link fullyQualifiedLink(URI baseUri, ContentStoreInfo store, Object id, String fieldName) {
		LinkBuilder builder = BasicLinkBuilder.linkToCurrentMapping();

		if (baseUri != null) {
			builder = builder.slash(baseUri);
		}

		String property = StringUtils.uncapitalize(ContentStoreUtils.propertyName(fieldName));
		return builder.slash(ContentStoreUtils.storePath(store))
				.slash(id)
				.slash(property)
				.withRel(property);
	}

	private Link propertyLink(ResourceMetadata md, URI baseUri, Object id, String property, String contentId) {
		LinkBuilder builder = new RepositoryLinkBuilder(md, new BaseUri(baseUri));

		builder = builder.slash(id).slash(property);

		if (contentId != null) {
			builder = builder.slash(contentId);
		}

		return builder.withRel(property);
	}
}
