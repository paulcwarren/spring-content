package internal.org.springframework.content.rest.utils;

import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

public final class RepositoryUtils {

	private RepositoryUtils() {}

	public static ResourceMetadata findRepositoryMapping(Repositories repositories, ResourceMappings repositoryMappings, String repository) {
		
		ResourceMetadata mapping = null;
		for (Class<?> domainType : repositories) {
			ResourceMetadata candidate = repositoryMappings.getMetadataFor(domainType);
			if (candidate.getPath().matches(repository)
					&& candidate.isExported()) {
				mapping = candidate;
				break;
			}
		}
		return mapping;
	}
	
	public static Object findDomainObject(RepositoryInvokerFactory repositoryInvokerFactory, Class<?> domainType, String id)
			throws HttpRequestMethodNotSupportedException {

		RepositoryInvoker repositoryInvoker = repositoryInvokerFactory.getInvokerFor(domainType);

		if (!repositoryInvoker.hasFindOneMethod()) {
			throw new HttpRequestMethodNotSupportedException("fineOne");
		}

		Object domainObj = repositoryInvoker.invokeFindOne(id);

		if (null == domainObj) {
			throw new ResourceNotFoundException();
		}

		return domainObj;
	}
}
