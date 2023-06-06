package org.springframework.content.rest.config;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import internal.org.springframework.content.rest.mappingcontext.ContentPropertyToExportedContext;
import internal.org.springframework.content.rest.mappingcontext.ContentPropertyToRequestMappingContext;
import internal.org.springframework.content.rest.mappingcontext.ContentPropertyToLinkrelMappingContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.storeservice.StoreResolver;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.rest.config.StoreCacheControlInterceptor.StoreCacheControlConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.support.DefaultRepositoryInvokerFactory;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import internal.org.springframework.content.commons.storeservice.StoresImpl;
import internal.org.springframework.content.rest.controllers.ResourceHandlerMethodArgumentResolver;
import internal.org.springframework.content.rest.controllers.resolvers.DefaultEntityResolver;
import internal.org.springframework.content.rest.controllers.resolvers.EntityResolvers;
import internal.org.springframework.content.rest.controllers.resolvers.RevisionEntityResolver;
import internal.org.springframework.content.rest.mappings.ContentHandlerMapping;
import internal.org.springframework.content.rest.mappings.StoreByteRangeHttpRequestHandler;

@Configuration
@ComponentScan("internal.org.springframework.content.rest.controllers, org.springframework.data.rest.extensions, org.springframework.data.rest.versioning")
public class RestConfiguration implements InitializingBean {

	public static boolean OVERWRITE_EXISTING_CONTENT_DEFAULT = true;
	public static SetContentDisposition SETCONTENT_CONTENT_DISPOSITION_DEFAULT = SetContentDisposition.Overwrite;
	public static UnsetContentDisposition UNSETCONTENT_CONTENT_DISPOSITION_DEFAULT = UnsetContentDisposition.Remove;
	public static boolean FULLY_QUALIFIED_DEFAULTS_DEFAULT = true;
    public static boolean SHORTCUT_LINKS_DEFAULT = true;

	private static final URI NO_URI = URI.create("");

	@Autowired
	private ApplicationContext context;

	@Autowired(required = false)
	private List<ContentRestConfigurer> configurers = new ArrayList<>();

	private URI baseUri = NO_URI;
	private StoreCorsRegistry corsRegistry;
	private boolean fullyQualifiedLinks = FULLY_QUALIFIED_DEFAULTS_DEFAULT;
    private boolean shortcutLinks = SHORTCUT_LINKS_DEFAULT;
	private boolean overwriteExistingContent = OVERWRITE_EXISTING_CONTENT_DEFAULT;
	private SetContentDisposition setContentDisposition = SETCONTENT_CONTENT_DISPOSITION_DEFAULT;
	private UnsetContentDisposition unsetContentDisposition = UNSETCONTENT_CONTENT_DISPOSITION_DEFAULT;
	private ConverterRegistry converters = new DefaultConversionService();

	private Map<Class<?>, DomainTypeConfig> domainTypeConfigMap = new HashMap<>();
	private Exclusions shortcutExclusions = new Exclusions();

    private StoreCacheControlInterceptor storeHandlerInterceptor;
    private StoresImpl stores;

	public RestConfiguration() {
		this.corsRegistry = new StoreCorsRegistry();
	}

	public URI getBaseUri() {
		return baseUri;
	}

	public void setBaseUri(URI baseUri) {
		this.baseUri = baseUri;
	}

	public boolean fullyQualifiedLinks() {
		return fullyQualifiedLinks;
	}

	public void setFullyQualifiedLinks(boolean fullyQualifiedLinks) {
		this.fullyQualifiedLinks = fullyQualifiedLinks;
	}

    public boolean shortcutLinks() {
        return shortcutLinks;
    }

    public void setShortcutLinks(boolean shortcutLinks) {
        this.shortcutLinks = shortcutLinks;
    }

	public boolean overwriteExistingContent() {
		return this.overwriteExistingContent;
	}

	public void setOverwriteExistingContent(boolean overwriteExistingContent) {
		this.overwriteExistingContent = overwriteExistingContent;
	}

	public SetContentDisposition getSetContentDisposition() {
		return setContentDisposition;
	}

	public void setSetContentDisposition(SetContentDisposition setContentDisposition) {
		this.setContentDisposition = setContentDisposition;
	}

	public UnsetContentDisposition getUnsetContentDisposition() {
		return unsetContentDisposition;
	}

	public void setUnsetContentDisposition(UnsetContentDisposition unsetContentDisposition) {
		this.unsetContentDisposition = unsetContentDisposition;
	}

	public StoreCorsRegistry getCorsRegistry() {
		return corsRegistry;
	}

    public StoreCacheControlInterceptor getStoreHandlerInterceptor() {
        if (this.storeHandlerInterceptor == null) {
            this.storeHandlerInterceptor = new StoreCacheControlInterceptor();
        }
        return this.storeHandlerInterceptor;
    }

    public Stores getStores() {
        if (this.stores == null) {
            Assert.notNull(context, "context should not be null");
            this.stores = new StoresImpl(this.context);
        }
        return this.stores;
    }

	public void addStoreResolver(String name, StoreResolver resolver) {
		this.getStores().addStoreResolver(name, resolver);
	}

	public StoreCacheControlConfigurer cacheControl() {
	    return this.getStoreHandlerInterceptor().configurer();
	}

	public DomainTypeConfig forDomainType(Class<?> type) {
		DomainTypeConfig config = domainTypeConfigMap.get(type);
		if (config  == null) {
			config = new DomainTypeConfig();
			domainTypeConfigMap.put(type, config);
		}
		return config;
	}

	public Exclusions shortcutExclusions() {
	    return this.shortcutExclusions;
	}

	public ConverterRegistry converters() {
	    return converters;
	}

	@Bean
	Stores stores() {
	    return this.getStores();
	}

	@Bean
	StoreCacheControlInterceptor storeHandlerInterceptor() {
	    return this.getStoreHandlerInterceptor();
	}

    @Bean
    MappingContext mappingContext() {
        return new MappingContext("/", ".");
    }

	@Bean
	ContentPropertyToRequestMappingContext exportedRequestMappingContext() {
		return new ContentPropertyToRequestMappingContext();
	}

	@Bean
	ContentPropertyToExportedContext exportedContentPropertyContext() {
		return new ContentPropertyToExportedContext();
	}

	@Bean
	ContentPropertyToLinkrelMappingContext linkrelMappingContext() {
		return new ContentPropertyToLinkrelMappingContext();
	}

	@Bean
	RequestMappingHandlerMapping contentHandlerMapping(Stores stores, EntityResolvers entityResolvers, ContentPropertyToRequestMappingContext exportedRequestMappingContext) {
		ContentHandlerMapping mapping = new ContentHandlerMapping(stores, entityResolvers, exportedRequestMappingContext, this);
		mapping.setCorsConfigurations(this.getCorsRegistry().getCorsConfigurations());
        mapping.setInterceptors(this.getStoreHandlerInterceptor());
		return mapping;
	}

	@Bean
	StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler() {
	    StoreByteRangeHttpRequestHandler handler = new StoreByteRangeHttpRequestHandler();

	    // implemented by StoreRestController
	    handler.setUseLastModified(false);

	    return handler;
	}

	@Bean
	EntityResolvers entityResolvers(ApplicationContext context, Stores stores, MappingContext mappingContext) {

	    Repositories repositories = null;
	    try {
	        repositories = context.getBean(Repositories.class);
	    } catch (NoSuchBeanDefinitionException nsbde) {
	        if (repositories == null) {
	            repositories = new Repositories(context);
	        }
	    }

	    EntityResolvers entityResolvers = new EntityResolvers();
	    entityResolvers.add(new DefaultEntityResolver(context, repositories, stores, (ConversionService)converters(), "/{repository}/{id}", mappingContext));
	    entityResolvers.add(new DefaultEntityResolver(context, repositories, stores, (ConversionService)converters(), "/{repository}/{id}/**", mappingContext));
        entityResolvers.add(new RevisionEntityResolver(repositories, stores, "/{repository}/{id}/revisions/{revisionId}", mappingContext));
        entityResolvers.add(new RevisionEntityResolver(repositories, stores, "/{repository}/{id}/revisions/{revisionId}/**", mappingContext));
	    return entityResolvers;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		for (ContentRestConfigurer configurer : configurers) {
			configurer.configure(this);
		}

        this.getStoreHandlerInterceptor().setBaseUri(baseUri);
	}

	@Configuration
	public static class WebConfig implements WebMvcConfigurer, InitializingBean {

		@Autowired
		private RestConfiguration config;

		@Autowired
		private ApplicationContext context;

		@Autowired(required = false)
		private Repositories repositories;

		@Autowired(required = false)
		private RepositoryInvokerFactory repoInvokerFactory;

		@Autowired
		private StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler;

		@Autowired
		private Stores stores;

		@Autowired
		private EntityResolvers entityResolvers;

		@Autowired
		private ContentPropertyToRequestMappingContext requestMappingContext;

		@Autowired
		private MappingContext mappingContext;

		@Override
		public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {

			argumentResolvers.add(new ResourceHandlerMethodArgumentResolver(context, config, repositories, stores, requestMappingContext, mappingContext, entityResolvers));
		}

		@Override
		public void afterPropertiesSet() throws Exception {

			if (repositories == null) {
				repositories = new Repositories(context);
			}

			if (repoInvokerFactory == null) {
				repoInvokerFactory = new DefaultRepositoryInvokerFactory(repositories);
			}
		}
	}

	public class DomainTypeConfig {

        private Resolver<Method, HttpHeaders> setContentResolver = new Resolver<Method, HttpHeaders>(){

            @Override
            public boolean resolve(Method method, HttpHeaders context) {
                return preferInputStream(method);
            }
        };

		public DomainTypeConfig(){}

		public Resolver<Method, HttpHeaders> getSetContentResolver() {
			return setContentResolver;
		}

        public void setSetContentResolver(Resolver<Method, HttpHeaders> resolver) {
            this.setContentResolver = resolver;
        }

        public void putAndPostPreferResource() {
			setContentResolver = new Resolver<Method, HttpHeaders>(){

	            @Override
	            public boolean resolve(Method method, HttpHeaders context) {
	                return preferResource(method);
	            }
	        };
		}

		/* package */ boolean preferResource(Method method) {
            for (Class<?> paramType : method.getParameterTypes()) {
                if (Resource.class.equals(paramType)) {
                    return true;
                }
            }
            return false;
		}

		/* package */ boolean preferInputStream(Method method) {

		    for (Class<?> paramType : method.getParameterTypes()) {
	            if (InputStream.class.equals(paramType)) {
	                return true;
	            }
		    }
			return false;
		}
	}

	public interface Resolver<S, C> {
	    boolean resolve(S subject, C context);
	}

	public static class Exclusions extends HashMap<String,List<MediaType>> {

        private static final long serialVersionUID = 5499454344207867070L;

        public Exclusions exclude(String method, MediaType mediaType) {
            Assert.hasLength(method, "method must not be null or empty");

            List<MediaType> excludes = this.get(method);
            if (excludes == null) {
                excludes = new ArrayList<>();
                this.put(method, excludes);
            }
            excludes.add(mediaType);

            return this;
        }
    }
}
