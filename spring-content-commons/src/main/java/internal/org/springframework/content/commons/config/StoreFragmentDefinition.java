package internal.org.springframework.content.commons.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.data.config.ConfigurationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

public class StoreFragmentDefinition {

	private final String interfaceName;
	private final String implementationClassName;
	private final BeanDefinition beanDefinition;

	private String storeInterfaceName;

	public StoreFragmentDefinition(String interfaceName, BeanDefinition beanDef) {
		this.interfaceName = interfaceName;
		this.implementationClassName = ConfigurationUtils.getRequiredBeanClassName(beanDef);
		this.beanDefinition = beanDef;
	}

	public void setStoreInterfaceName(String storeInterface) {
		this.storeInterfaceName = storeInterface;
	}

	public String getStoreInterfaceName() {
		return this.storeInterfaceName;
	}

	public String getInterfaceName() {
		return interfaceName;
	}

	public String getImplementationBeanName() {
		return this.storeInterfaceName + "#" + StringUtils.uncapitalize(ClassUtils.getShortName(implementationClassName));
	}

	public BeanDefinition getBeanDefinition() {
		return beanDefinition;
	}

	public String getFragmentBeanName() {
		return getImplementationBeanName() + "Fragment";
	}

}
