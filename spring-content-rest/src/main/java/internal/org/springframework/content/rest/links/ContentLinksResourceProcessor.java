package internal.org.springframework.content.rest.links;

import static java.lang.String.format;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import internal.org.springframework.content.rest.io.StoreResource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import internal.org.springframework.content.rest.mappingcontext.ContentPropertyToRequestMappingContext;
import internal.org.springframework.content.rest.mappingcontext.ContentPropertyToLinkrelMappingContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.content.commons.mappingcontext.ContentProperty;
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

	private static Method GET_CONTENT_METHOD = ReflectionUtils.findMethod(StoreRestController.class, "getContent", HttpServletRequest.class, HttpServletResponse.class, HttpHeaders.class, StoreResource.class);

	static {
		Assert.notNull(GET_CONTENT_METHOD, "Unable to find StoreRestController.getContent method");
	}

	private final ContentPropertyToRequestMappingContext requestMappingContext;
	private final ContentPropertyToLinkrelMappingContext linkrelMappingContext;

	private Stores stores;
	private RestConfiguration config;
	private MappingContext mappingContext;

	public ContentLinksResourceProcessor(Stores stores, RestConfiguration config, MappingContext mappingContext, ContentPropertyToRequestMappingContext requestMappingContext, ContentPropertyToLinkrelMappingContext linkrelMappingContext) {
		this.stores = stores;
		this.config = config;
		this.mappingContext = mappingContext;
		this.requestMappingContext = requestMappingContext;
		this.linkrelMappingContext = linkrelMappingContext;
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

        Map<String, ContentProperty> contentProperties = mappingContext.getContentPropertyMap(persistentEntityType);

		if(contentProperties.size() == 1 && config.shortcutLinks() && !config.fullyQualifiedLinks()) {
			// for compatibility with v0.x.0 versions
			originalLink(config.getBaseUri(), store, entityId).ifPresent((l) -> addLink(resource, l));

			addLink(resource, shortcutLink(config.getBaseUri(), store, entityId, StringUtils
					.uncapitalize(StoreUtils.getSimpleName(store))));
		} else {
            for (Map.Entry<String, ContentProperty> contentProperty : contentProperties.entrySet()) {
                resource.add(fullyQualifiedLink(config.getBaseUri(), store, entityId, contentProperty));
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

	private String propertyLinkRel(StoreInfo storeInfo, Map.Entry<String, ContentProperty> contentProperty) {
//        String uriPath = StringUtils.uncapitalize(contentProperty.getKey());
		String uriPath = contentProperty.getKey();
		Map<String,String> linkrelMappings = this.linkrelMappingContext.getMappings(storeInfo.getDomainObjectClass());
		String linkrel = linkrelMappings.get(uriPath);
		if (linkrel == null || !StringUtils.hasLength(linkrel)) {
			linkrel = uriPath;
		}

		Class<?> storeIface = storeInfo.getInterface();
		StoreRestResource exportSpec = storeIface.getAnnotation(StoreRestResource.class);
		if (exportSpec != null && !StringUtils.isEmpty(exportSpec.linkRel())) {
			linkrel = exportSpec.linkRel() + "/" + linkrel;
		}

		return linkrel;
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

	private Link fullyQualifiedLink(URI baseUri, StoreInfo store, Object id, Map.Entry<String, ContentProperty> contentPropertyEntry) {

        Assert.notNull(id);

        LinkBuilder builder = StoreLinkBuilder.linkTo(new BaseUri(baseUri), store);

        builder = builder.slash(id);

		String requestMapping = requestMappingContext.getMappings(store.getDomainObjectClass()).get(contentPropertyEntry.getKey());
		if (StringUtils.hasText(requestMapping)) {
			builder = builder.slash(requestMapping);
		} else {
			builder = builder.slash(contentPropertyEntry.getKey());
		}

        return builder.withRel(propertyLinkRel(store, contentPropertyEntry));
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
