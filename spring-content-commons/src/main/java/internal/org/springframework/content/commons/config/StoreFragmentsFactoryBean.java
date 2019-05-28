package internal.org.springframework.content.commons.config;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public class StoreFragmentsFactoryBean implements FactoryBean<StoreFragments>, BeanFactoryAware, InitializingBean {

	private final List<String> fragmentBeanNames;
	private BeanFactory beanFactory;
	private StoreFragments storeFragments;

	public StoreFragmentsFactoryBean(List<String> fragmentBeanNames) {

		Assert.notNull(fragmentBeanNames, "Fragment bean names must not be null!");
		this.fragmentBeanNames = fragmentBeanNames;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() {

		List<StoreFragment> fragments = fragmentBeanNames.stream()
				.map(it -> beanFactory.getBean(it, StoreFragment.class))
				.collect(Collectors.toList());

		storeFragments = new StoreFragments(fragments);
	}


	@Override
	public StoreFragments getObject() throws Exception {
		return storeFragments;
	}

	@Override
	public Class<?> getObjectType() {
		return StoreFragments.class;
	}
}
