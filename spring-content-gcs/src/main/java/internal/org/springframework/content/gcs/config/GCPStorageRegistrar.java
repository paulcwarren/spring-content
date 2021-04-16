package internal.org.springframework.content.gcs.config;

import java.lang.annotation.Annotation;

import org.springframework.content.commons.config.AbstractStoreBeanDefinitionRegistrar;
import org.springframework.content.gcs.config.EnableGCPStorage;
import org.springframework.content.gcs.store.GCPStorageContentStore;

public class GCPStorageRegistrar extends AbstractStoreBeanDefinitionRegistrar {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableGCPStorage.class;
	}

    @Override
    protected Class<?>[] getSignatureTypes() {
        return new Class[]{GCPStorageContentStore.class};
    }

    @Override
    protected String getOverridePropertyValue() {
        return "gs";
    }
}
