package internal.org.springframework.content.common.utils;

import java.beans.Introspector;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.content.common.config.ContentRepositoriesConfigurationSource;
import org.springframework.content.common.config.ContentRepositoryConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

public class DefaultContentRepositoryConfiguration<T extends ContentRepositoriesConfigurationSource> 
				implements ContentRepositoryConfiguration<T> {

	private final T configurationSource;
	private final BeanDefinition definition;

	/**
	 * Creates a new {@link DefaultRepositoryConfiguration} from the given {@link RepositoryConfigurationSource} and
	 * source {@link BeanDefinition}.
	 * 
	 * @param configurationSource must not be {@literal null}.
	 * @param definition must not be {@literal null}.
	 */
	public DefaultContentRepositoryConfiguration(T configurationSource, BeanDefinition definition) {

		Assert.notNull(configurationSource);
		Assert.notNull(definition);

		this.configurationSource = configurationSource;
		this.definition = definition;
	}

	public String getRepositoryBeanName() {
		String beanName = ClassUtils.getShortName(definition.getBeanClassName());
		return Introspector.decapitalize(beanName);
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getRepositoryFactoryBeanName()
	 */
	public String getRepositoryFactoryBeanName() {
		return configurationSource.getRepositoryFactoryBeanName();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getRepositoryInterface()
	 */
	public String getRepositoryInterface() {
		return definition.getBeanClassName();
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getSource()
	 */
	public Object getSource() {
		return configurationSource.getSource();
	}
}
