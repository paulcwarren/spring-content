package internal.org.springframework.content.mongo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import internal.org.springframework.content.mongo.repository.DefaultMongoStoreImpl;

public class MongoStoreFactoryBean extends AbstractStoreFactoryBean {

	@Autowired private GridFsTemplate gridFs;
	@Autowired private ConversionService mongoStoreConverter;

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
	}

	@Override
	protected Object getContentStoreImpl() {
		return new DefaultMongoStoreImpl(gridFs, mongoStoreConverter);
	}
}
