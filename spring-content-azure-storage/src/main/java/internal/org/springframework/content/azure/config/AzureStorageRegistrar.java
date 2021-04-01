package internal.org.springframework.content.azure.config;

import java.lang.annotation.Annotation;

import org.springframework.content.azure.config.EnableAzureStorage;
import org.springframework.content.azure.store.AzureStorageContentStore;
import org.springframework.content.commons.config.AbstractStoreBeanDefinitionRegistrar;

public class AzureStorageRegistrar extends AbstractStoreBeanDefinitionRegistrar {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableAzureStorage.class;
	}

    @Override
    protected Class<?>[] getSignatureTypes() {
        return new Class[]{AzureStorageContentStore.class};
    }

    @Override
    protected String getOverridePropertyValue() {
        return "azs";
    }
}
