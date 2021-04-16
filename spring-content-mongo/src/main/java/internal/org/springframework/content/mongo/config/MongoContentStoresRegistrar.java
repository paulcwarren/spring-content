package internal.org.springframework.content.mongo.config;

import java.lang.annotation.Annotation;

import org.springframework.content.commons.config.AbstractStoreBeanDefinitionRegistrar;
import org.springframework.content.mongo.config.EnableMongoStores;
import org.springframework.content.mongo.store.MongoContentStore;

public class MongoContentStoresRegistrar extends AbstractStoreBeanDefinitionRegistrar {

    @Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableMongoStores.class;
	}

	@Override
	protected Class<?>[] getSignatureTypes() {
		return new Class[]{MongoContentStore.class};
	}

    @Override
    protected String getOverridePropertyValue() {
        return "gridfs";
    }
}
