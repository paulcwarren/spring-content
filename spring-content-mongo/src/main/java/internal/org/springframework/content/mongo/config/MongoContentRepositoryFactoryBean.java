package internal.org.springframework.content.mongo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.factory.AbstractContentStoreFactoryBean;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import internal.org.springframework.content.mongo.store.DefaultMongoContentStoreImpl;

public class MongoContentRepositoryFactoryBean extends AbstractContentStoreFactoryBean {

	private GridFsTemplate gridFs;

	@Autowired
	public void setGridFs(GridFsTemplate gridFs) {
		this.gridFs = gridFs;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
	}

	@Override
	protected Object getContentStoreImpl() {
		return new DefaultMongoContentStoreImpl(gridFs);
	}

}
