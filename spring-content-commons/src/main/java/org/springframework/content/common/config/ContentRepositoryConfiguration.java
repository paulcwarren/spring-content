package org.springframework.content.common.config;

import org.springframework.beans.factory.FactoryBean;

public interface ContentRepositoryConfiguration<T extends ContentRepositoriesConfigurationSource> {

	/**
	 * Returns the name of the ContentStore bean.
	 * 
	 * @return
	 */
	public String getRepositoryBeanName();

	/**
	 * Returns the name of the {@link FactoryBean} class to be used to create repository instances.
	 * 
	 * @return
	 */
	public String getRepositoryFactoryBeanName();

	/**
	 * Returns the interface name of the repository.
	 * 
	 * @return
	 */
	public String getRepositoryInterface();
	
	/**
	 * Returns the source of the {@link RepositoryConfiguration}.
	 * 
	 * @return
	 */
	public Object getSource();
	
}
