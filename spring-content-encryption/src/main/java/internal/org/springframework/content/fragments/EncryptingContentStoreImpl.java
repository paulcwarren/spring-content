package internal.org.springframework.content.fragments;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.fragments.ContentStoreAware;
import org.springframework.content.commons.io.RangeableResource;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.*;
import org.springframework.content.commons.utils.AssertUtils;
import org.springframework.content.encryption.EnvelopeEncryptionService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.util.Pair;
import org.springframework.util.Assert;

import javax.crypto.CipherInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class EncryptingContentStoreImpl<S, SID extends Serializable> implements ContentStore<S, SID>, org.springframework.content.commons.store.ContentStore<S, SID>, ContentStoreAware {

    @Autowired(required = false)
    private MappingContext mappingContext = null;

    @Autowired
    private EnvelopeEncryptionService encrypter;

    @Autowired(required = false)
    private List<EncryptingContentStoreConfigurer> configurers;

    private String encryptionKeyContentProperty = "key";

    private String keyRing = "shared-key";

    private ContentStore delegate;

    private org.springframework.content.commons.store.ContentStore storeDelegate;

    private Class<?> domainClass;

    public EncryptingContentStoreImpl() {
    }

    protected MappingContext getMappingContext() {
        if (this.mappingContext == null) {
            this.mappingContext = new MappingContext("/", ".");
        }
        return mappingContext;
    }

    @Override
    public S setContent(S o, InputStream inputStream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream content) {
        Assert.notNull(entity, "entity not set");
        Assert.notNull(propertyPath, "propertyPath not set");
        Assert.notNull(content, "content not set");
        AssertUtils.atLeastOneNotNull(new Object[]{storeDelegate, delegate}, "store not set");

        ContentProperty contentProperty = getMappingContext().getContentProperty(entity.getClass(), propertyPath.getName());
        if (contentProperty == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        Pair<CipherInputStream, byte[]> encryptionContext = encrypter.encrypt(content, this.keyRing);
        contentProperty.setCustomProperty(entity, this.encryptionKeyContentProperty, encryptionContext.getSecond());
        if (storeDelegate != null) {
            return (S) storeDelegate.setContent(entity, propertyPath, encryptionContext.getFirst());
        } else if (delegate != null) {
            return (S) delegate.setContent(entity, propertyPath, encryptionContext.getFirst());
        }
        throw new IllegalStateException("no store set");
    }

    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream inputStream, long l) {
        AssertUtils.atLeastOneNotNull(new Object[] {storeDelegate, delegate}, "store not set");
        if (storeDelegate != null) {
            return this.setContent(entity, propertyPath, inputStream, org.springframework.content.commons.store.SetContentParams.builder().contentLength(l).build());
        }
        return this.setContent(entity, propertyPath, inputStream, SetContentParams.builder().contentLength(l).build());
    }

    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream content, SetContentParams params) {
        Assert.notNull(entity, "entity not set");
        Assert.notNull(propertyPath, "propertyPath not set");
        Assert.notNull(content, "content not set");
        Assert.notNull(params, "params not set");
        Assert.notNull(delegate, "store not set");

        ContentProperty contentProperty = getMappingContext().getContentProperty(entity.getClass(), propertyPath.getName());
        if (contentProperty == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        Pair<CipherInputStream, byte[]> encryptionContext = encrypter.encrypt(content, this.keyRing);
        contentProperty.setCustomProperty(entity, this.encryptionKeyContentProperty, encryptionContext.getSecond());
        return (S) delegate.setContent(entity, propertyPath, encryptionContext.getFirst(), params);
    }

    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream content, org.springframework.content.commons.store.SetContentParams params) {
        Assert.notNull(entity, "entity not set");
        Assert.notNull(propertyPath, "propertyPath not set");
        Assert.notNull(content, "content not set");
        Assert.notNull(params, "params not set");
        Assert.notNull(storeDelegate, "store not set");

        ContentProperty contentProperty = getMappingContext().getContentProperty(entity.getClass(), propertyPath.getName());
        if (contentProperty == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        Pair<CipherInputStream, byte[]> encryptionContext = encrypter.encrypt(content, this.keyRing);
        contentProperty.setCustomProperty(entity, this.encryptionKeyContentProperty, encryptionContext.getSecond());
        return (S) storeDelegate.setContent(entity, propertyPath, encryptionContext.getFirst(), params);
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
        AssertUtils.atLeastOneNotNull(new Object[]{storeDelegate, delegate}, "store not set");

        try {
            ContentProperty contentProperty = getMappingContext().getContentProperty(entity.getClass(), propertyPath.getName());
            if (contentProperty == null) {
                throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
            }

            Pair<CipherInputStream, byte[]> encryptionContext = null;
            encryptionContext = encrypter.encrypt(resource.getInputStream(), this.keyRing);
            contentProperty.setCustomProperty(entity, this.encryptionKeyContentProperty, encryptionContext.getSecond());
            if (storeDelegate != null) {
                return (S) delegate.setContent(entity, propertyPath, new InputStreamResource(encryptionContext.getFirst()));
            }
            return (S) delegate.setContent(entity, propertyPath, new InputStreamResource(encryptionContext.getFirst()));
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
        return this.unsetContent(entity, propertyPath, org.springframework.content.commons.store.UnsetContentParams.builder().build());
    }

    @Override
    public S unsetContent(S entity, PropertyPath propertyPath, UnsetContentParams params) {
        int ordinal = params.getDisposition().ordinal();
        org.springframework.content.commons.store.UnsetContentParams params1 = org.springframework.content.commons.store.UnsetContentParams.builder()
                .disposition(org.springframework.content.commons.store.UnsetContentParams.Disposition.values()[ordinal])
                .build();
        return this.unsetContent(entity, propertyPath, params1);
    }


    @Override
    public S unsetContent(S entity, PropertyPath propertyPath, org.springframework.content.commons.store.UnsetContentParams params) {
        Assert.notNull(entity, "entity not set");
        Assert.notNull(propertyPath, "propertyPath not set");
        AssertUtils.atLeastOneNotNull(new Object[]{storeDelegate, delegate}, "store not set");

        ContentProperty contentProperty = getMappingContext().getContentProperty(entity.getClass(), propertyPath.getName());
        if (contentProperty == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        S entityToReturn = null;
        if (storeDelegate != null) {
            entityToReturn = (S) storeDelegate.unsetContent(entity, propertyPath, params);
        } else if (delegate != null) {
            int ordinal = params.getDisposition().ordinal();
            org.springframework.content.commons.repository.UnsetContentParams params1 = org.springframework.content.commons.repository.UnsetContentParams.builder()
                    .disposition(org.springframework.content.commons.repository.UnsetContentParams.Disposition.values()[ordinal])
                    .build();
            entityToReturn = (S) delegate.unsetContent(entity, propertyPath, params1);
        }

        contentProperty.setCustomProperty(entity, this.encryptionKeyContentProperty, null);

        return entityToReturn;
    }

    @Override
    public InputStream getContent(S o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getContent(S entity, PropertyPath propertyPath) {
        Assert.notNull(entity, "entity not set");
        Assert.notNull(propertyPath, "propertyPath not set");
        AssertUtils.atLeastOneNotNull(new Object[]{storeDelegate, delegate}, "store not set");

        InputStream encryptedContentStream = null;
        if (storeDelegate != null) {
            encryptedContentStream = delegate.getContent(entity, propertyPath);
        } else if (delegate != null) {
            encryptedContentStream = delegate.getContent(entity, propertyPath);
        }

        InputStream unencryptedStream = null;
        if (encryptedContentStream != null) {

            ContentProperty contentProperty = getMappingContext().getContentProperty(entity.getClass(), propertyPath.getName());
            if (contentProperty == null) {
                throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
            }

            // remove cast and use conversion service
            unencryptedStream = encrypter.decrypt((byte[]) contentProperty.getCustomProperty(entity, this.encryptionKeyContentProperty), encryptedContentStream, 0, this.keyRing);
        }

        return unencryptedStream;
    }

    @Override
    public Resource getResource(S o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource getResource(S entity, PropertyPath propertyPath) {
        Assert.notNull(entity, "entity not set");
        Assert.notNull(propertyPath, "propertyPath not set");
        AssertUtils.atLeastOneNotNull(new Object[]{storeDelegate, delegate}, "store not set");

        Resource r = null;
        if (storeDelegate != null) {
            r = storeDelegate.getResource(entity, propertyPath);
        } else if (delegate != null) {
            r = delegate.getResource(entity, propertyPath);
        }

        if (r != null) {
            InputStream unencryptedStream = null;
            try {
                ContentProperty contentProperty = getMappingContext().getContentProperty(entity.getClass(), propertyPath.getName());
                if (contentProperty == null) {
                    throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
                }

                // remove cast and use conversion service
                unencryptedStream = encrypter.decrypt((byte[]) contentProperty.getCustomProperty(entity, this.encryptionKeyContentProperty), r.getInputStream(), 0, this.keyRing);
                r = new InputStreamResource(unencryptedStream);
            } catch (IOException e) {
                throw new StoreAccessException("error encrypting resource", e);
            }
        }

        return r;
    }

    @Override
    public Resource getResource(S entity, PropertyPath propertyPath, org.springframework.content.commons.store.GetResourceParams params) {
        Assert.notNull(entity, "entity not set");
        Assert.notNull(propertyPath, "propertyPath not set");
        Assert.notNull(storeDelegate, "store not set");

        Resource r = storeDelegate.getResource(entity, propertyPath, params);

        if (r != null) {
            InputStream unencryptedStream = null;
            try {
                ContentProperty contentProperty = getMappingContext().getContentProperty(entity.getClass(), propertyPath.getName());
                if (contentProperty == null) {
                    throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
                }

                // remove cast and use conversion service
                unencryptedStream = encrypter.decrypt((byte[]) contentProperty.getCustomProperty(entity, this.encryptionKeyContentProperty), r.getInputStream(), getOffset(r, params), this.keyRing);
                r = new InputStreamResource(unencryptedStream);
            } catch (IOException e) {
                throw new StoreAccessException("error encrypting resource", e);
            }
        }

        return r;
    }

    @Override
    public Resource getResource(S entity, PropertyPath propertyPath, GetResourceParams params) {
        Assert.notNull(entity, "entity not set");
        Assert.notNull(propertyPath, "propertyPath not set");
        Assert.notNull(delegate, "store not set");

        Resource r = delegate.getResource(entity, propertyPath, params);

        if (r != null) {
            InputStream unencryptedStream = null;
            try {
                ContentProperty contentProperty = getMappingContext().getContentProperty(entity.getClass(), propertyPath.getName());
                if (contentProperty == null) {
                    throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
                }

                // remove cast and use conversion service
                unencryptedStream = encrypter.decrypt((byte[]) contentProperty.getCustomProperty(entity, this.encryptionKeyContentProperty), r.getInputStream(), getOffset(r, params), this.keyRing);
                r = new InputStreamResource(unencryptedStream);
            } catch (IOException e) {
                throw new StoreAccessException("error encrypting resource", e);
            }
        }

        return r;
    }

    private int getOffset(Resource r, GetResourceParams params) {
        int offset = 0;

        if (r instanceof RangeableResource == false)
            return offset;
        if (params.getRange() == null)
            return offset;

        return Integer.parseInt(StringUtils.substringBetween(params.getRange(), "bytes=", "-"));
    }

    private int getOffset(Resource r, org.springframework.content.commons.store.GetResourceParams params) {
        int offset = 0;

        if (r instanceof RangeableResource == false)
            return offset;
        if (params.getRange() == null)
            return offset;

        return Integer.parseInt(StringUtils.substringBetween(params.getRange(), "bytes=", "-"));
    }

    @Override
    public void associate(S o, SID serializable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void associate(S o, PropertyPath propertyPath, SID serializable) {
        this.associate(o, propertyPath, serializable);
    }

    @Override
    public void unassociate(S o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unassociate(S o, PropertyPath propertyPath) {
        this.unassociate(o, propertyPath);
    }

    @Override
    public Resource getResource(SID id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDomainClass(Class<?> domainClass) {
        this.domainClass = domainClass;
    }

    @Override
    public void setIdClass(Class<?> idClass) {
    }

    @Override
    public void setContentStore(ContentStore store) {
        this.delegate = store;
    }

    @Override
    public void setContentStore(org.springframework.content.commons.store.ContentStore store) {
        this.storeDelegate = store;
    }

    public void setStoreInterfaceClass(Class<? extends Store> storeInterfaceClass) {
        configure(storeInterfaceClass);
    }

    private void configure(Class<? extends Store> storeInterfaceClass) {
        if (configurers == null)
            return;

        for (EncryptingContentStoreConfigurer configurer : configurers) {
            Optional<?> interfaces = Arrays.stream(configurer.getClass().getGenericInterfaces()).findFirst();
            if (interfaces.isPresent() == false)
                continue;

            Type[] genericArguments = ((ParameterizedType)interfaces.get()).getActualTypeArguments();
            if (genericArguments.length >= 1 == false)
                continue;

            if (genericArguments[0].equals(storeInterfaceClass)) {
                EncryptingContentStoreConfigurationImpl config = new EncryptingContentStoreConfigurationImpl();
                configurer.configure(config);
                this.encryptionKeyContentProperty = config.getEncryptionKeyContentProperty();
                this.keyRing = config.getKeyring();
            }
        }
    }

    public class EncryptingContentStoreConfigurationImpl implements EncryptingContentStoreConfiguration {
        private String encryptionKeyContentProperty;
        private String keyring;

        @Override
        public EncryptingContentStoreConfiguration encryptionKeyContentProperty(String encryptionKeyContentProperty) {
            this.encryptionKeyContentProperty = encryptionKeyContentProperty;
            return this;
        }

        @Override
        public EncryptingContentStoreConfiguration keyring(String keyring) {
            this.keyring = keyring;
            return this;
        }

        /*package*/ String getEncryptionKeyContentProperty() {
            return this.encryptionKeyContentProperty;
        }

        /*package*/ String getKeyring() {
            return this.keyring;
        }
    }
}
