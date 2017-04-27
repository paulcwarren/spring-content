package internal.org.springframework.content.jpa.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;
import org.springframework.util.Assert;

import internal.org.springframework.content.jpa.operations.JpaContentTemplate;
import internal.org.springframework.content.jpa.repository.DefaultJpaStoreImpl;

@SuppressWarnings("rawtypes")
public class JpaStoreFactoryBean extends AbstractStoreFactoryBean {

	@Autowired 
	private JpaContentTemplate template;
	
	@Override
	protected Object getContentStoreImpl() {
		Assert.notNull(template, "template cannot be null");
		return new DefaultJpaStoreImpl(template);
	}

}
