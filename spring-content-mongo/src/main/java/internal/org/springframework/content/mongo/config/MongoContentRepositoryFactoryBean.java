package internal.org.springframework.content.mongo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.factory.AbstractContentStoreFactoryBean;

import internal.org.springframework.content.mongo.MongoContentTemplate;
import internal.org.springframework.content.mongo.repository.DefaultMongoContentRepositoryImpl;

public class MongoContentRepositoryFactoryBean extends AbstractContentStoreFactoryBean {

	private MongoContentTemplate template;

	@Autowired
	public void setMongoContentTemplate(MongoContentTemplate template) {
		this.template = template;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
	}

	@Override
	protected Object getContentStoreImpl() {
		return new DefaultMongoContentRepositoryImpl(template);
	}
}
