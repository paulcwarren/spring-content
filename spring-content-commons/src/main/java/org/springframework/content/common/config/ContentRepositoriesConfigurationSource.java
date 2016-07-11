package org.springframework.content.common.config;

import org.springframework.beans.factory.FactoryBean;

/**
 * Overall Spring Content Repositories configuration.  Similar in nature to Spring Data's 
 * RepositoryConfigurationSource
 * 
 * @author warrep
 */
public interface ContentRepositoriesConfigurationSource {

	/**
	 * Returns the base packages the repository interfaces shall be found under.
	 * 
	 * @return must not be {@literal null}.
	 */
	Iterable<String> getBasePackages();
	
	/**
	 * Returns the name of the class of the {@link FactoryBean} to actually create repository instances.
	 * 
	 * @return
	 */
	String getRepositoryFactoryBeanName();

	/**
	 * Returns the actual source object that the configuration originated from. Will be used by the tooling to give visual
	 * feedback on where the repository instances actually come from.
	 * 
	 * @return must not be {@literal null}.
	 */
	Object getSource();
}
