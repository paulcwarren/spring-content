package internal.org.springframework.content.mongo.config;

import internal.org.springframework.content.mongo.repository.DefaultMongoStoreImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

public class MongoStoreFactoryBean extends AbstractStoreFactoryBean {

	@Autowired
	private GridFsTemplate gridFs;
	@Autowired
	private PlacementService mongoStorePlacementService;

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
	}

	@Override
	protected Object getContentStoreImpl() {
		return new DefaultMongoStoreImpl(gridFs, mongoStorePlacementService);
	}
}
