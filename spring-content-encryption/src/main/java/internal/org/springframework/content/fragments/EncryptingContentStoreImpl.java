package internal.org.springframework.content.fragments;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.fragments.ContentStoreAware;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.encryption.EnvelopeEncryptionService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.util.Pair;
import org.springframework.util.Assert;

import javax.crypto.CipherInputStream;
import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class EncryptingContentStoreImpl<S, SID extends Serializable> implements ContentStore<S, SID>, ContentStoreAware {

    private MappingContext mappingContext = null;
    @Autowired
    private EnvelopeEncryptionService encrypter;

    @Autowired(required = false)
    private List<EncryptingContentStoreConfigurer> configurers;

    private String encryptionKeyContentProperty = "key";

    private String keyRing = "shared-key";

    private ContentStore delegate;

    private Class<?> domainClass;

    @Autowired
    public EncryptingContentStoreImpl(MappingContext mappingContext) {
        if (this.mappingContext == null) {
            this.mappingContext = new MappingContext("/", ".");
        }
    }

    @Override
    public S setContent(S o, InputStream inputStream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public S setContent(S o, PropertyPath propertyPath, InputStream inputStream) {
        Assert.notNull(o);
        Assert.notNull(propertyPath);
        Assert.notNull(inputStream);

        ContentProperty contentProperty = mappingContext.getContentProperty(o.getClass(), propertyPath.getName());
        if (contentProperty == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        Pair<CipherInputStream, byte[]> encryptionContext = encrypter.encrypt(inputStream, this.keyRing);
        contentProperty.setCustomProperty(o, this.encryptionKeyContentProperty, encryptionContext.getSecond());
        return (S) delegate.setContent(o, propertyPath, encryptionContext.getFirst());
    }

    @Override
    public S setContent(S o, PropertyPath propertyPath, InputStream inputStream, long l) {
        Assert.notNull(o);
        Assert.notNull(propertyPath);
        Assert.notNull(inputStream);

        ContentProperty contentProperty = mappingContext.getContentProperty(o.getClass(), propertyPath.getName());
        if (contentProperty == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        Pair<CipherInputStream, byte[]> encryptionContext = encrypter.encrypt(inputStream, this.keyRing);
        contentProperty.setCustomProperty(o, this.encryptionKeyContentProperty, encryptionContext.getSecond());
        return (S) delegate.setContent(o, propertyPath, encryptionContext.getFirst(), l);
    }

    @Override
    public S setContent(S o, Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public S setContent(S o, PropertyPath propertyPath, Resource resource) {
        Assert.notNull(o);
        Assert.notNull(propertyPath);
        Assert.notNull(resource);

        try {
            ContentProperty contentProperty = mappingContext.getContentProperty(o.getClass(), propertyPath.getName());
            if (contentProperty == null) {
                throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
            }

            Pair<CipherInputStream, byte[]> encryptionContext = null;
            encryptionContext = encrypter.encrypt(resource.getInputStream(), this.keyRing);
            contentProperty.setCustomProperty(o, this.encryptionKeyContentProperty, encryptionContext.getSecond());
            return (S) delegate.setContent(o, propertyPath, new InputStreamResource(encryptionContext.getFirst()));
        } catch (IOException e) {
            throw new StoreAccessException("error encrypting resource", e);
        }
    }

    @Override
    public S unsetContent(S o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public S unsetContent(S o, PropertyPath propertyPath) {
        Assert.notNull(o);
        Assert.notNull(propertyPath);

        ContentProperty contentProperty = mappingContext.getContentProperty(o.getClass(), propertyPath.getName());
        if (contentProperty == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        S entityToReturn = (S) delegate.unsetContent(o, propertyPath);

        contentProperty.setCustomProperty(o, this.encryptionKeyContentProperty, null);

        return entityToReturn;
    }

    @Override
    public InputStream getContent(S o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getContent(S o, PropertyPath propertyPath) {
        Assert.notNull(o);
        Assert.notNull(propertyPath);

        InputStream encryptedContentStream = delegate.getContent(o, propertyPath);

        CipherInputStream unencryptedStream = null;
        if (encryptedContentStream != null) {

            ContentProperty contentProperty = mappingContext.getContentProperty(o.getClass(), propertyPath.getName());
            if (contentProperty == null) {
                throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
            }

            // remove cast and use conversion service
            unencryptedStream = encrypter.decrypt((byte[]) contentProperty.getCustomProperty(o, this.encryptionKeyContentProperty), encryptedContentStream, this.keyRing);
        }

        return unencryptedStream;
    }

    @Override
    public Resource getResource(S o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource getResource(S o, PropertyPath propertyPath) {
        Assert.notNull(o);
        Assert.notNull(propertyPath);

        Resource r = delegate.getResource(o, propertyPath);

        if (r != null) {
            CipherInputStream unencryptedStream = null;
            try {
                ContentProperty contentProperty = mappingContext.getContentProperty(o.getClass(), propertyPath.getName());
                if (contentProperty == null) {
                    throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
                }

                // remove cast and use conversion service
                unencryptedStream = encrypter.decrypt((byte[]) contentProperty.getCustomProperty(o, this.encryptionKeyContentProperty), r.getInputStream(), this.keyRing);
                r = new InputStreamResource(new SkipInputStream(unencryptedStream));
            } catch (IOException e) {
                throw new StoreAccessException("error encrypting resource", e);
            }
        }

        return r;
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

    // CipherInputStream skip does not work.  This wraps a cipherinputstream purely to override the skip with a
    // working version
    public class SkipInputStream extends FilterInputStream
    {
        private static final int MAX_SKIP_BUFFER_SIZE = 2048;

        protected SkipInputStream (InputStream in)
        {
            super(in);
        }

        public long skip(long n)
                throws IOException
        {
            long remaining = n;
            int nr;

            if (n <= 0) {
                return 0;
            }

            int size = (int)Math.min(MAX_SKIP_BUFFER_SIZE, remaining);
            byte[] skipBuffer = new byte[size];
            while (remaining > 0) {
                nr = in.read(skipBuffer, 0, (int)Math.min(size, remaining));
                if (nr < 0) {
                    break;
                }
                remaining -= nr;
            }

            return n - remaining;
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
