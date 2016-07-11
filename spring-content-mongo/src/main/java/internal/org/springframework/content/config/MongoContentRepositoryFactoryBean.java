package internal.org.springframework.content.config;

import internal.org.springframework.content.store.DefaultMongoContentStoreImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.common.repository.factory.AbstractContentStoreFactoryBean;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

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
