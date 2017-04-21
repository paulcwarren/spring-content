package org.springframework.content.commons.operations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

public abstract class AbstractResourceTemplate implements ResourceOperations {
	
	private static Log logger = LogFactory.getLog(AbstractResourceTemplate.class);
	
	private ResourceLoader resourceLoader;
	
	public AbstractResourceTemplate(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}
	
    @Override
	public Resource get(String location) {
		Resource resource = resourceLoader.getResource(location);
		return resource;
	}

	@Override
	public void delete(Resource resource) {
		try {
			if (resource.exists()) {
				this.deleteResource(resource);
			}
		} catch (Exception ase) {
			logger.error(String.format("Unexpected error unsetting content %s", resource.getFilename()), ase);
		}
	}


	// hook for our placement strategy
	protected abstract String getLocation(Object contentId);


	// todo: remove this
	protected abstract void deleteResource(Resource resource) throws Exception;

}
