package org.springframework.content.commons.config;

import org.springframework.beans.factory.FactoryBean;

public interface ContentRepositoryConfiguration<T extends ContentRepositoriesConfigurationSource> {

	/**
	 * Returns the name of the ContentStore bean.
	 * 
	 * @return the name of the content store bean
	 */
	public String getRepositoryBeanName();

	/**
	 * Returns the name of the {@link FactoryBean} class to be used to create content repository instances.
	 * 
	 * @return the content repository factory bean class 
	 */
	public String getRepositoryFactoryBeanName();

	/**
	 * Returns the interface name of the content repository.
	 * 
	 * @return the content repository interface name
	 */
	public String getRepositoryInterface();
	
	/**
	 * Returns the source of the {@link ContentRepositoryConfiguration}.
	 * 
	 * @return the source of the content repository configuration
	 */
	public Object getSource();
	
}
