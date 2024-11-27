package internal.org.springframework.content.fragments;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.fragments.ContentStoreAware;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.SetContentParams;
import org.springframework.content.commons.repository.UnsetContentParams;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.SetContentParams.ContentDisposition;
import org.springframework.content.commons.store.Store;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.UnsetContentParams.Disposition;
import org.springframework.content.encryption.config.EncryptingContentStoreConfigurer;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

public class EncryptingContentStoreImpl<S, SID extends Serializable> implements
        org.springframework.content.commons.store.ContentStore<S, SID>,
        org.springframework.content.commons.repository.ContentStore<S, SID>,
        ContentStoreAware {

    private final MappingContext mappingContext;
    private ContentCryptoService<S, ?> cryptoService;
    private final List<EncryptingContentStoreConfigurer<S>> configurers;

    private ContentStore<S, SID> storeDelegate;

    @Autowired
    public EncryptingContentStoreImpl(
            @Autowired(required = false) MappingContext mappingContext,
            List<EncryptingContentStoreConfigurer<S>> configurers
    ) {
        this.mappingContext = Optional.ofNullable(mappingContext).orElseGet(() -> new MappingContext("/", "."));
        this.configurers = configurers;
    }

    @Override
    public S setContent(S o, InputStream inputStream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream content) {
        Assert.notNull(storeDelegate, "store not set");
        Assert.notNull(cryptoService, "cryptoService not set");

        return cryptoService.encrypt(entity, propertyPath, content,
                (e, s) -> storeDelegate.setContent(e, propertyPath, s));
    }

    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream inputStream, long l) {
        return this.setContent(entity, propertyPath, inputStream,
                org.springframework.content.commons.store.SetContentParams.builder().contentLength(l).build());
    }

    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream content, SetContentParams params) {
        return setContent(entity, propertyPath, content, convertParams(params));
    }

    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream content, org.springframework.content.commons.store.SetContentParams params) {
        Assert.notNull(storeDelegate, "store not set");
        Assert.notNull(cryptoService, "cryptoService not set");

        return cryptoService.encrypt(entity, propertyPath, content,
                (e, s) -> storeDelegate.setContent(e, propertyPath, s, params));
    }

    @Override
    public S setContent(S o, Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public S setContent(S entity, PropertyPath propertyPath, Resource resource) {
        Assert.notNull(entity, "entity not set");
        Assert.notNull(propertyPath, "propertyPath not set");
        Assert.notNull(resource, "resource not set");
        Assert.notNull(storeDelegate, "store not set");
        Assert.notNull(cryptoService, "cryptoService not set");

        try {
            return cryptoService.encrypt(entity, propertyPath, resource.getInputStream(),
                    (e, s) -> storeDelegate.setContent(e, propertyPath, new InputStreamResource(s)));
        } catch (IOException e) {
            throw new StoreAccessException("error encrypting resource", e);
        }
    }

    @Override
    public S unsetContent(S o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public S unsetContent(S entity, PropertyPath propertyPath) {
        return unsetContent(entity, propertyPath,
                org.springframework.content.commons.store.UnsetContentParams.builder().build());
    }

    @Override
    public S unsetContent(S entity, PropertyPath propertyPath, UnsetContentParams params) {
        return unsetContent(entity, propertyPath, convertParams(params));
    }

    @Override
    public S unsetContent(S entity, PropertyPath propertyPath, org.springframework.content.commons.store.UnsetContentParams params) {
        Assert.notNull(entity, "entity not set");
        Assert.notNull(propertyPath, "propertyPath not set");
        Assert.notNull(storeDelegate, "store not set");
        Assert.notNull(cryptoService, "cryptoService not set");

        ContentProperty contentProperty = mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (contentProperty == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        S newEntity = storeDelegate.unsetContent(entity, propertyPath, params);

        return cryptoService.clearKeys(newEntity, propertyPath);
    }

    @Override
    public InputStream getContent(S o) {
        throw new UnsupportedOperationException();
    }

    @SneakyThrows(IOException.class)
    @Override
    public InputStream getContent(S entity, PropertyPath propertyPath) {
        Assert.notNull(entity, "entity not set");
        Assert.notNull(propertyPath, "propertyPath not set");
        Assert.notNull(storeDelegate, "store not set");
        Assert.notNull(cryptoService, "cryptoService not set");

        return cryptoService.decrypt(entity, propertyPath, null, () -> storeDelegate.getResource(entity, propertyPath))
                .getInputStream();
    }

    @Override
    public Resource getResource(S o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource getResource(S entity, PropertyPath propertyPath) {
        Assert.notNull(entity, "entity not set");
        Assert.notNull(propertyPath, "propertyPath not set");
        Assert.notNull(storeDelegate, "store not set");
        Assert.notNull(cryptoService, "cryptoService not set");

        return cryptoService.decrypt(entity, propertyPath, null, () -> storeDelegate.getResource(entity, propertyPath));
    }

    @Override
    public Resource getResource(S entity, PropertyPath propertyPath,
            org.springframework.content.commons.repository.GetResourceParams params) {
        return getResource(entity, propertyPath, convertParams(params));
    }

    @Override
    public Resource getResource(S entity, PropertyPath propertyPath, GetResourceParams params) {
        Assert.notNull(entity, "entity not set");
        Assert.notNull(propertyPath, "propertyPath not set");
        Assert.notNull(storeDelegate, "store not set");
        Assert.notNull(cryptoService, "cryptoService not set");

        return cryptoService.decrypt(entity, propertyPath, params,
                () -> storeDelegate.getResource(entity, propertyPath));
    }

    @Override
    public void associate(S o, SID serializable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void associate(S o, PropertyPath propertyPath, SID serializable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unassociate(S o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unassociate(S o, PropertyPath propertyPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource getResource(SID id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDomainClass(Class<?> domainClass) {
    }

    @Override
    public void setIdClass(Class<?> idClass) {
    }

    @Override
    public void setContentStore(org.springframework.content.commons.repository.ContentStore store) {
    }

    @Override
    public void setContentStore(ContentStore store) {
        this.storeDelegate = store;
    }

    public void setStoreInterfaceClass(Class<? extends Store<?>> storeInterfaceClass) {
        configure(storeInterfaceClass);
    }

    private void configure(Class<? extends Store<?>> storeInterfaceClass) {
        if (configurers == null)
            return;

        EncryptingContentStoreConfigurationImpl config = new EncryptingContentStoreConfigurationImpl();
        for (EncryptingContentStoreConfigurer<?> configurer : configurers) {
            Optional<?> interfaces = Arrays.stream(configurer.getClass().getGenericInterfaces()).findFirst();
            if (interfaces.isEmpty())
                continue;

            Type[] genericArguments = ((ParameterizedType)interfaces.get()).getActualTypeArguments();
            if (genericArguments.length < 1)
                continue;

            if (genericArguments[0].equals(storeInterfaceClass)) {
                configurer.configure(config);
            }
        }

        cryptoService = config.initializeCryptoService(mappingContext);
    }

    private static org.springframework.content.commons.store.UnsetContentParams convertParams(
            UnsetContentParams params) {
        return org.springframework.content.commons.store.UnsetContentParams.builder()
                .disposition(convertDisposition(params.getDisposition()))
                .build();
    }

    private static Disposition convertDisposition(UnsetContentParams.Disposition disposition) {
        return switch (disposition) {
            case Keep -> Disposition.Keep;
            case Remove -> Disposition.Remove;
        };
    }

    private GetResourceParams convertParams(org.springframework.content.commons.repository.GetResourceParams params) {
        return GetResourceParams.builder()
                .range(params.getRange())
                .build();
    }

    private static org.springframework.content.commons.store.SetContentParams convertParams(SetContentParams params) {
        return org.springframework.content.commons.store.SetContentParams.builder()
                .contentLength(params.getContentLength())
                .disposition(convertDisposition(params.getDisposition()))
                .overwriteExistingContent(params.isOverwriteExistingContent())
                .build();
    }

    private static ContentDisposition convertDisposition(SetContentParams.ContentDisposition disposition) {
        return switch (disposition) {
            case Overwrite -> ContentDisposition.Overwrite;
            case CreateNew -> ContentDisposition.CreateNew;
        };
    }
}
