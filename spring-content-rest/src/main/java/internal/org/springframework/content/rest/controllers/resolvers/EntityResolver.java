package internal.org.springframework.content.rest.controllers.resolvers;

import java.util.Map;
import java.util.Optional;

import org.springframework.data.repository.support.RepositoryInvoker;

import internal.org.springframework.content.rest.controllers.ResourceNotFoundException;

public class EntityResolver {

    private RepositoryInvoker invoker;

    public EntityResolver(RepositoryInvoker invoker) {
        this.invoker = invoker;
    }

    public Object resolve(Map<String,String> variables) {

        Optional<Object> domainObj = invoker.invokeFindById(variables.get("id"));
        return domainObj.orElseThrow(ResourceNotFoundException::new);
    }
}
