package internal.org.springframework.content.rest.links;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import internal.org.springframework.content.rest.controllers.ContentEntityRestController;
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
import org.springframework.hateoas.server.LinkBuilder;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import static java.lang.String.format;

/**
 * Adds content and content collection links to Spring Data REST Entity Resources.
 *
 * @author warrep
 *
 */
public class ContentLinksResourceProcessor implements RepresentationModelProcessor<PersistentEntityResource> {

	private static final Log log = LogFactory.getLog(ContentLinksResourceProcessor.class);

	private static Method GET_CONTENT_METHOD = ReflectionUtils.findMethod(ContentEntityRestController.class, "getContent", HttpServletRequest.class, HttpServletResponse.class, String.class, String.class, String.class);

	static {
		Assert.notNull(GET_CONTENT_METHOD, "Unable to find ContentEntityRestController.getContent method");
	}


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

				// for compatibility with v0.x.0 versions
				originalLink(ContentEntityRestController.class, GET_CONTENT_METHOD, config.getBaseUri(), store, entityId).ifPresent((l) -> resource.add(l));

				resource.add(shortcutLink(ContentEntityRestController.class, GET_CONTENT_METHOD, config.getBaseUri(), store, entityId, StringUtils.uncapitalize(ContentStoreUtils.getSimpleName(store))));
			}
		} else if (fields.length > 1) {
			for (Field field : fields) {
				resource.add(fullyQualifiedLink(ContentEntityRestController.class, GET_CONTENT_METHOD, config.getBaseUri(), store, entityId, field.getName()));
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
				log.trace(format("No field for property %s, ignoring",
						descriptor.getName()));
			}
			catch (SecurityException se) {
				log.warn(format(
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
							log.trace(format("Didn't get value for property %s", field.getName()));
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
						log.trace(format("Didn't get value for property %s", field.getName()));
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

	private Optional<Link> originalLink(Class<?> controller, Method method, URI baseUri, ContentStoreInfo store, Object id) {

		if (id == null) {
			return Optional.empty();
		}

		return Optional.of(shortcutLink(controller, method, baseUri, store, id, ContentStoreUtils.storePath(store)));
	}

	private Link shortcutLink(Class<?> controller, Method method, URI baseUri, ContentStoreInfo store, Object id, String rel) {
        LinkBuilder builder = null;

        String storePath = ContentStoreUtils.storePath(store);
        if (!StringUtils.isEmpty(baseUri.toString())) {
            String basePath = baseUri.toString();
            if (basePath.startsWith("/")) {
                basePath = StringUtils.trimLeadingCharacter(basePath, '/');
            }

            storePath = format("%s/%s", basePath, storePath);
        }

        builder = WebMvcLinkBuilder.linkTo(controller, method, storePath, id);

        return builder.withRel(rel);
	}

	private Link fullyQualifiedLink(Class<?> controller, Method method, URI baseUri, ContentStoreInfo store, Object id, String fieldName) {
        LinkBuilder builder = null;

        String storePath = ContentStoreUtils.storePath(store);
        if (!StringUtils.isEmpty(baseUri.toString())) {
            String basePath = baseUri.toString();
            if (basePath.startsWith("/")) {
                basePath = StringUtils.trimLeadingCharacter(basePath, '/');
            }

            storePath = format("%s/%s", basePath, storePath);
        }

		String property = StringUtils.uncapitalize(ContentStoreUtils.propertyName(fieldName));
        builder = WebMvcLinkBuilder.linkTo(controller, method, storePath, id, property);

        return builder.withRel(property);
	}

	private Link propertyLink(ResourceMetadata md, URI baseUri, Object id, String property, String contentId) {
		LinkBuilder builder = new RepositoryLinkBuilder(md, new BaseUri(baseUri));

		builder = builder.slash(id).slash(property);

		if (contentId != null) {
			builder = builder.slash(contentId);
		}

		return builder.withRel(property);
	}

	protected Object invokeField(Field field, Object object) {

		try {
			return field.get(object);
		}
		catch (IllegalAccessException e) {
			return null;
		}
	}
}
