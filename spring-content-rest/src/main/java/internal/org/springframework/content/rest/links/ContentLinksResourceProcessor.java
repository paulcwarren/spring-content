package internal.org.springframework.content.rest.links;

import static java.lang.String.format;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.commons.utils.DomainObjectUtils;
import org.springframework.content.rest.StoreRestResource;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.core.io.Resource;
import org.springframework.data.projection.TargetAware;
import org.springframework.data.rest.webmvc.BaseUri;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.hateoas.Affordance;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.LinkBuilder;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.server.core.LinkBuilderSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;

import internal.org.springframework.content.rest.controllers.StoreRestController;
import internal.org.springframework.content.rest.utils.StoreUtils;

/**
 * Adds content and content collection links to Spring Data REST Entity Resources.
 *
 * @author warrep
 *
 */
public class ContentLinksResourceProcessor implements RepresentationModelProcessor<PersistentEntityResource> {

	private static final Log log = LogFactory.getLog(ContentLinksResourceProcessor.class);

	private static Method GET_CONTENT_METHOD = ReflectionUtils.findMethod(StoreRestController.class, "getContent", HttpServletRequest.class, HttpServletResponse.class, HttpHeaders.class, Resource.class);

	static {
		Assert.notNull(GET_CONTENT_METHOD, "Unable to find StoreRestController.getContent method");
	}

	private Stores stores;
	private RestConfiguration config;
	private MappingContext mappingContext;

	public ContentLinksResourceProcessor(Stores stores, RestConfiguration config, MappingContext mappingContext) {
		this.stores = stores;
		this.config = config;
		this.mappingContext = mappingContext;
	}

	RestConfiguration getRestConfiguration() {
		return config;
	}

	@Override
    public PersistentEntityResource process(final PersistentEntityResource resource) {

		Object object = resource.getContent();
		if (object == null)
			return resource;

		if (isProjection(object)) {
            object = getProjectionTarget(object);
        }

		Object entityId = DomainObjectUtils.getId(object);
		if(entityId == null) {
			// If there is no entity ID, we can't have content links (because they reference the entity ID)
			return resource;
		}

		Class<?> persistentEntityType = resource.getPersistentEntity().getType();
		StoreInfo store = stores.getStore(AssociativeStore.class, Stores.withDomainClass(persistentEntityType));
		if(store == null) {
			// If there is no store, this PersistentEntityResource can't have content links
			return resource;
		}
		Collection<String> contentPropertyPaths = mappingContext.getContentPaths(persistentEntityType);

		if(contentPropertyPaths.size() == 1 && config.shortcutLinks() && !config.fullyQualifiedLinks()) {
			// for compatibility with v0.x.0 versions
			originalLink(config.getBaseUri(), store, entityId).ifPresent((l) -> addLink(resource, l));

			addLink(resource, shortcutLink(config.getBaseUri(), store, entityId, StringUtils
					.uncapitalize(StoreUtils.getSimpleName(store))));
		} else {
			for (String contentPropertyPath: contentPropertyPaths) {
				if(!contentPropertyPath.isEmpty()) {
					resource.add(fullyQualifiedLink(config.getBaseUri(), store, entityId, contentPropertyPath));
				}
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

	private String propertyLinkRel(StoreInfo storeInfo, String name) {
		String contentRel = StringUtils.uncapitalize(name);
		Class<?> storeIface = storeInfo.getInterface();
		StoreRestResource exportSpec = storeIface.getAnnotation(StoreRestResource.class);
		if (exportSpec != null && !StringUtils.isEmpty(exportSpec.linkRel())) {
			contentRel = exportSpec.linkRel();
		}
		return contentRel;
	}

	private String entityRel(StoreInfo storeInfo, String defaultLinkRel) {
		String entityLinkRel = defaultLinkRel;
		Class<?> storeIface = storeInfo.getInterface();
		StoreRestResource exportSpec = storeIface.getAnnotation(StoreRestResource.class);
		if (exportSpec != null && !StringUtils.isEmpty(exportSpec.linkRel())) {
			entityLinkRel = exportSpec.linkRel();
		}
		return entityLinkRel;
	}

	private Optional<Link> originalLink(URI baseUri, StoreInfo store, Object id) {

		if (id == null) {
			return Optional.empty();
		}

		return Optional.of(shortcutLink(baseUri, store, id, StoreUtils.storePath(store)));
	}

	private Link shortcutLink(URI baseUri, StoreInfo store, Object id, String defaultLinkRel) {

		LinkBuilder builder = null;
		builder = StoreLinkBuilder.linkTo(new BaseUri(baseUri), store);

		builder = builder.slash(id);

		return builder.withRel(entityRel(store, defaultLinkRel));
	}

	private Link fullyQualifiedLink(URI baseUri, StoreInfo store, Object id, String contentPropertyPath) {

	    Assert.notNull(id);

		LinkBuilder builder = StoreLinkBuilder.linkTo(new BaseUri(baseUri), store);

		builder = builder.slash(id);

		builder = builder.slash(contentPropertyPath);

		return builder.withRel(propertyLinkRel(store, contentPropertyPath));
	}

    private Object getProjectionTarget(Object object) {
        return ((TargetAware)object).getTarget();
    }

    private boolean isProjection(Object object) {
        return AopUtils.isAopProxy(object);
    }

	public static class StoreLinkBuilder extends LinkBuilderSupport<StoreLinkBuilder> {

		public StoreLinkBuilder(BaseUri baseUri, StoreInfo store) {
			super(baseUri.getUriComponentsBuilder().path(storePath(store)).build());
		}

		@Override
		protected StoreLinkBuilder getThis() {
			return this;
		}

		@Override
		protected StoreLinkBuilder createNewInstance(UriComponents components, List<Affordance> affordances) {
			return new StoreLinkBuilder(new BaseUri(components.toUriString()), null);
		}

		public static StoreLinkBuilder linkTo(BaseUri baseUri, StoreInfo store) {
			return new StoreLinkBuilder(baseUri, store);
		}

		private static String storePath(StoreInfo store) {
			if (store == null) {
				return "";
			}

			return format("/%s", StoreUtils.storePath(store));
		}
	}
}
