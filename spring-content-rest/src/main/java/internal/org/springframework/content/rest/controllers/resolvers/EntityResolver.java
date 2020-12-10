package internal.org.springframework.content.rest.controllers.resolvers;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import internal.org.springframework.content.rest.controllers.ResourceNotFoundException;
import internal.org.springframework.content.rest.utils.RepositoryUtils;

public class EntityResolver {

    private RepositoryInvokerFactory factory;
    private Repositories repositories;
    private StoreInfo storeInfo;

    public EntityResolver(RepositoryInvokerFactory factory, Repositories repositories, StoreInfo storeInfo) {
        this.factory = factory;
        this.repositories = repositories;
        this.storeInfo = storeInfo;
    }

    public Object resolve(Map<String,String> variables) {
        String repository = variables.get("repository");
        String id = variables.get("id");

        Object domainObj = null;
        try {
            try {
                domainObj = findOne(factory, repositories, storeInfo.getDomainObjectClass(), id);
            } catch (IllegalArgumentException iae) {
                domainObj = findOne(factory, repositories, repository, id);
            }
        }
        catch (HttpRequestMethodNotSupportedException e) {
            throw new ResourceNotFoundException();
        }

        return domainObj;
    }

    public Object findOne(RepositoryInvokerFactory repoInvokerFactory, Repositories repositories, String repository, String id)
            throws HttpRequestMethodNotSupportedException {

        RepositoryInformation ri = RepositoryUtils.findRepositoryInformation(repositories, repository);

        if (ri == null) {
            throw new ResourceNotFoundException();
        }

        Class<?> domainObjClazz = ri.getDomainType();

        return findOne(repoInvokerFactory, repositories, domainObjClazz, id);
    }

    public Object findOne(RepositoryInvokerFactory repoInvokerFactory, Repositories repositories, Class<?> domainObjClass, String id)
            throws HttpRequestMethodNotSupportedException {

        Optional<Object> domainObj = null;

        if (repoInvokerFactory != null) {

            RepositoryInvoker invoker = repoInvokerFactory.getInvokerFor(domainObjClass);
            if (invoker != null) {
                domainObj = invoker.invokeFindById(id);
            }
        } else {

            RepositoryInformation ri = RepositoryUtils.findRepositoryInformation(repositories, domainObjClass);

            if (ri == null) {
                throw new ResourceNotFoundException();
            }

            Class<?> domainObjClazz = ri.getDomainType();
            Class<?> idClazz = ri.getIdType();

            Optional<Method> findOneMethod = ri.getCrudMethods().getFindOneMethod();
            if (!findOneMethod.isPresent()) {
                throw new HttpRequestMethodNotSupportedException("fineOne");
            }

            Object oid = new DefaultConversionService().convert(id, idClazz);
            domainObj = (Optional<Object>) ReflectionUtils.invokeMethod(findOneMethod.get(),
                    repositories.getRepositoryFor(domainObjClazz).get(),
                    oid);
        }
        return domainObj.orElseThrow(ResourceNotFoundException::new);
    }
}
