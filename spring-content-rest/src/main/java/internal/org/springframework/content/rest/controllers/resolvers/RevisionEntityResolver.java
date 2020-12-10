package internal.org.springframework.content.rest.controllers.resolvers;

import static java.lang.String.format;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.core.io.Resource;
import org.springframework.data.history.Revision;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import internal.org.springframework.content.rest.controllers.ResourceNotFoundException;
import internal.org.springframework.content.rest.io.AssociatedResource;
import internal.org.springframework.content.rest.io.AssociatedResourceImpl;
import internal.org.springframework.content.rest.io.RenderableResourceImpl;

public class RevisionEntityResolver {

    private RepositoryInvokerFactory factory;
    private Repositories repositories;
    private Stores stores;
    private StoreInfo storeInfo;

    public RevisionEntityResolver(RepositoryInvokerFactory factory, Repositories repositories, Stores stores, StoreInfo storeInfo) {
        this.factory = factory;
        this.repositories = repositories;
        this.stores = stores;
        this.storeInfo = storeInfo;
    }

    public Object resolve(Map<String,String> variables) {

        String repository = variables.get("repository");
        String id = variables.get("id");
        String revisionId = variables.get("revisionId");
        String contentProperty = variables.get("property");
        String contentPropertyId = variables.get("contentId");

        Method FIND_REVISION_METHOD = ReflectionUtils.findMethod(RevisionRepository.class, "findRevision", Object.class, Number.class);
        Assert.notNull(FIND_REVISION_METHOD, "findRevision method cannot be null");

        Optional<Object> repo = repositories.getRepositoryFor(storeInfo.getDomainObjectClass());
        repo.orElseThrow(() -> new IllegalStateException(format("Unable to find repository '%s'", repository)));

        Optional<Revision<?,?>> revision = (Optional<Revision<?, ?>>) ReflectionUtils.invokeMethod(FIND_REVISION_METHOD, repo.get(), Long.parseLong(id), Integer.parseInt(revisionId));

        Object domainObj = null;
        if (revision.isPresent()) {
            domainObj = revision.get().getEntity();
        } else {
            throw new ResourceNotFoundException();
        }

        return domainObj;
    }

    protected Resource resolve(StoreInfo i, Object e, Object p, boolean propertyIsEmbedded) {

        AssociativeStore s = i.getImplementation(AssociativeStore.class);
        Resource resource = s.getResource(p);
        resource = new AssociatedResourceImpl(p, resource);
        if (Renderable.class.isAssignableFrom(i.getInterface())) {
            resource = new RenderableResourceImpl((Renderable)i.getImplementation(AssociativeStore.class), (AssociatedResource)resource);
        }
        return resource;
    }

    private Object instantiate(Class<?> clazz) {

        Object newObject = null;
        try {
            newObject = clazz.newInstance();
        }
        catch (InstantiationException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return newObject;
    }
}
