package internal.org.springframework.content.rest.links;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import internal.org.springframework.content.rest.controllers.StoreRestController;
import internal.org.springframework.content.rest.utils.ContentStoreUtils;
import internal.org.springframework.content.rest.utils.DomainObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.rest.StoreRestResource;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.core.io.Resource;
import org.springframework.data.rest.webmvc.BaseUri;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.LinkBuilder;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.server.core.LinkBuilderSupport;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import static java.lang.String.format;

/**
 * Adds content and content collection links to Spring Data REST Entity Resources.
 *
 * @author warrep
 *
 */
public class ContentLinksResourceProcessor implements RepresentationModelProcessor<PersistentEntityResource> {

	private static final Log log = LogFactory.getLog(ContentLinksResourceProcessor.class);

	private static Method GET_CONTENT_METHOD = ReflectionUtils.findMethod(StoreRestController.class, "getContent", HttpServletRequest.class, HttpServletResponse.class, String.class, Resource.class, MediaType.class, Object.class);

	static {
		Assert.notNull(GET_CONTENT_METHOD, "Unable to find StoreRestController.getContent method");
	}


	private ContentStoreService stores;
	private RestConfiguration config;

	public ContentLinksResourceProcessor(ContentStoreService stores, RestConfiguration config) {
		this.stores = stores;
		this.config = config;
	}

	RestConfiguration getRestConfiguration() {
		return config;
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

				if (config.fullyQualifiedLinks()) {
					resource.add(fullyQualifiedLink(config.getBaseUri(), store, entityId, fields[0].getName()));
				}
				else {
					// for compatibility with v0.x.0 versions
					originalLink(config.getBaseUri(), store, entityId).ifPresent((l) -> addLink(resource, l));

					addLink(resource, shortcutLink(config.getBaseUri(), store, entityId, StringUtils
							.uncapitalize(ContentStoreUtils.getSimpleName(store))));
				}
			}
		} else if (fields.length > 1) {
			for (Field field : fields) {
				resource.add(fullyQualifiedLink(config.getBaseUri(), store, entityId, field.getName()));
			}
		}

		return resource;
	}

	private void addLink(PersistentEntityResource resource, Link l) {

		if (resource.hasLink(l.getRel())) {
			for (Link existingLink : resource.getLinks(l.getRel())) {
				if (existingLink.getHref().equals(l.getHref())) {
					return;
				}
			}
		}

		resource.add(l);
	}

	private String propertyLinkRel(ContentStoreInfo storeInfo, String name) {
		String contentRel = StringUtils.uncapitalize(ContentStoreUtils.propertyName(name));
		Class<?> storeIface = storeInfo.getInterface();
		StoreRestResource exportSpec = storeIface.getAnnotation(StoreRestResource.class);
		if (exportSpec != null && !StringUtils.isEmpty(exportSpec.linkRel())) {
			contentRel = exportSpec.linkRel();
		}
		return contentRel;
	}

	private String entityRel(ContentStoreInfo storeInfo, String defaultLinkRel) {
		String entityLinkRel = defaultLinkRel;
		Class<?> storeIface = storeInfo.getInterface();
		StoreRestResource exportSpec = storeIface.getAnnotation(StoreRestResource.class);
		if (exportSpec != null && !StringUtils.isEmpty(exportSpec.linkRel())) {
			entityLinkRel = exportSpec.linkRel();
		}
		return entityLinkRel;
	}

	private Optional<Link> originalLink(URI baseUri, ContentStoreInfo store, Object id) {

		if (id == null) {
			return Optional.empty();
		}

		return Optional.of(shortcutLink(baseUri, store, id, ContentStoreUtils.storePath(store)));
	}

	private Link shortcutLink(URI baseUri, ContentStoreInfo store, Object id, String defaultLinkRel) {

		LinkBuilder builder = null;
		builder = StoreLinkBuilder.linkTo(new BaseUri(baseUri), store);

		builder = builder.slash(id);

		return builder.withRel(entityRel(store, defaultLinkRel));
	}

	private Link fullyQualifiedLink(URI baseUri, ContentStoreInfo store, Object id, String fieldName) {
		LinkBuilder builder = StoreLinkBuilder.linkTo(new BaseUri(baseUri), store);

		builder = builder.slash(id);

		String property = StringUtils.uncapitalize(ContentStoreUtils.propertyName(fieldName));
		builder = builder.slash(property);

		return builder.withRel(propertyLinkRel(store, fieldName));
	}

	public static class StoreLinkBuilder extends LinkBuilderSupport<StoreLinkBuilder> {

		public StoreLinkBuilder(BaseUri baseUri, ContentStoreInfo store) {
			super(baseUri.getUriComponentsBuilder().path(storePath(store)).build());
		}

		@Override
		protected StoreLinkBuilder getThis() {
			return this;
		}

		@Override
		protected StoreLinkBuilder createNewInstance(UriComponentsBuilder builder, List list) {
			return new StoreLinkBuilder(new BaseUri(builder.toUriString()), null);
		}

		public static StoreLinkBuilder linkTo(BaseUri baseUri, ContentStoreInfo store) {
			return new StoreLinkBuilder(baseUri, store);
		}

		private static String storePath(ContentStoreInfo store) {
			if (store == null) {
				return "";
			}
			
			return format("/%s", ContentStoreUtils.storePath(store));
		}
	}
}
