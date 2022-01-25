package internal.org.springframework.content.rest.controllers.resolvers;

import static java.lang.String.format;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.data.history.Revision;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.support.Repositories;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import internal.org.springframework.content.rest.controllers.ResourceNotFoundException;
import internal.org.springframework.content.rest.utils.StoreUtils;

public class RevisionEntityResolver implements EntityResolver {

    private Repositories repositories;
    private Stores stores;
    private String mapping;
    private MappingContext mappingContext;

    public RevisionEntityResolver(Repositories repositories, Stores stores, String mapping, MappingContext mappingContext) {
        this.repositories = repositories;
        this.stores = stores;
        this.mapping = mapping;
        this.mappingContext = mappingContext;
    }

    @Override
    public String getMapping() {
        return this.mapping;
    }

    @Override
    public EntityResolution resolve(String pathInfo) {

        AntPathMatcher matcher = new AntPathMatcher();
        Map<String,String> variables = matcher.extractUriTemplateVariables(this.mapping, pathInfo);
        String repository = variables.get("repository");
        String id = variables.get("id");
        String revisionId = variables.get("revisionId");

        Method FIND_REVISION_METHOD = ReflectionUtils.findMethod(RevisionRepository.class, "findRevision", Object.class, Number.class);
        Assert.notNull(FIND_REVISION_METHOD, "findRevision method cannot be null");

        String[] pathSegments = pathInfo.split("/");
        String store = pathSegments[1];

        StoreInfo info = this.stores.getStore(Store.class, StoreUtils.withStorePath(store));
        if (info == null) {
            throw new IllegalArgumentException(String.format("Store for path %s not found", store));
        }

        Optional<Object> repo = repositories.getRepositoryFor(info.getDomainObjectClass());
        repo.orElseThrow(() -> new IllegalStateException(format("Unable to find repository '%s'", repository)));

        Optional<Revision<?,?>> revision = (Optional<Revision<?, ?>>) ReflectionUtils.invokeMethod(FIND_REVISION_METHOD, repo.get(), Long.parseLong(id), Integer.parseInt(revisionId));

        Object domainObj = null;
        if (revision.isPresent()) {
            domainObj = revision.get().getEntity();
        } else {
            throw new ResourceNotFoundException();
        }

        String propertyPath = matcher.extractPathWithinPattern(this.mapping, pathInfo);
        if (propertyPath == null) {
            propertyPath = "";
        }
        ContentProperty property = mappingContext.getContentProperty(info.getDomainObjectClass(), propertyPath);

        return new EntityResolution(domainObj, property);
    }


    @Override
    public boolean hasPropertyFor(String pathInfo) {

        AntPathMatcher matcher = new AntPathMatcher();
        Map<String,String> variables = matcher.extractUriTemplateVariables(this.mapping, pathInfo);

        String[] pathSegments = pathInfo.split("/");
        String store = pathSegments[1];

        StoreInfo info = this.stores.getStore(Store.class, StoreUtils.withStorePath(store));
        if (info == null) {
            throw new IllegalArgumentException(String.format("Store for path %s not found", store));
        }

        String propertyPath = matcher.extractPathWithinPattern(this.mapping, pathInfo);
        if (propertyPath == null) {
            propertyPath = "";
        }
        return mappingContext.getContentProperty(info.getDomainObjectClass(), propertyPath) != null;
    }
}
