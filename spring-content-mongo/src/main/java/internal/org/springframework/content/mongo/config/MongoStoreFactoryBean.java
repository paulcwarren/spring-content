package internal.org.springframework.content.mongo.config;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.versions.LockingAndVersioningProxyFactory;

import internal.org.springframework.content.mongo.store.DefaultMongoStoreImpl;

public class MongoStoreFactoryBean extends AbstractStoreFactoryBean {

	@Autowired
	private GridFsTemplate gridFs;

	@Autowired
	private PlacementService mongoStorePlacementService;

    @Autowired(required=false)
    private MappingContext mappingContext;

	@Autowired(required=false)
    private LockingAndVersioningProxyFactory versioning;

	protected MongoStoreFactoryBean(Class<? extends Store> storeInterface) {
		super(storeInterface);
	}

	@Override
    protected void addProxyAdvice(ProxyFactory result, BeanFactory beanFactory) {
        if (versioning != null) {
            versioning.apply(result);
        }
    }

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
	}

	@Override
	protected Object getContentStoreImpl() {
		return new DefaultMongoStoreImpl(gridFs, mappingContext, mongoStorePlacementService);
	}
}
