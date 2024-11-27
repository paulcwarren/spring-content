package internal.org.springframework.content.encryption.keys;

import java.util.ArrayList;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.encryption.keys.DataEncryptionKeyAccessor;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

@RequiredArgsConstructor
public class ContentPropertyDataEncryptionKeyAccessor<S, T extends StoredDataEncryptionKey> implements DataEncryptionKeyAccessor<S, T> {

    private final String customPropertyName;
    private final ConversionService conversionService;

    private static final TypeDescriptor ENCRYPTED_DEK = new TypeDescriptor(
            ResolvableType.forClass(StoredDataEncryptionKey.class), null, null);
    private static final TypeDescriptor ENCRYPTED_DEK_COLLECTION = TypeDescriptor.collection(Collection.class,
            ENCRYPTED_DEK);

    @Override
    public Collection<T> findKeys(S entity, ContentProperty contentProperty) {
        var propertyValue = contentProperty.getCustomProperty(entity, customPropertyName);
        return (Collection<T>) conversionService.convert(propertyValue, ENCRYPTED_DEK_COLLECTION);
    }

    @Override
    public S clearKeys(S entity, ContentProperty contentProperty) {
        contentProperty.setCustomProperty(entity, customPropertyName, null);
        return entity;
    }

    @Override
    public S setKeys(S entity, ContentProperty contentProperty, Collection<T> newKeys) {
        var propertyPath = contentProperty.getCustomPropertyPropertyPath(customPropertyName);
        var beanWrapper = new BeanWrapperImpl(entity);
        var descriptor = beanWrapper.getPropertyDescriptor(propertyPath);
        var typeDescriptor = new TypeDescriptor(ResolvableType.forMethodParameter(descriptor.getWriteMethod(), 0), null, descriptor.getWriteMethod().getAnnotations());

        var keyType = newKeys.stream().findFirst().map(TypeDescriptor::forObject)
                .orElse(ENCRYPTED_DEK);

        var newValue = conversionService.convert(newKeys, TypeDescriptor.collection(newKeys.getClass(), keyType),
                typeDescriptor);
        contentProperty.setCustomProperty(entity, customPropertyName, newValue);
        return entity;
    }

    @Override
    public S addKey(S entity, ContentProperty contentProperty, T dataEncryptionKey) {
        var keys = new ArrayList<>(findKeys(entity, contentProperty));
        keys.remove(dataEncryptionKey);

        return setKeys(entity, contentProperty, keys);
    }

    @Override
    public S removeKey(S entity, ContentProperty contentProperty, T dataEncryptionKey) {
        var keys = new ArrayList<>(findKeys(entity, contentProperty));
        keys.add(dataEncryptionKey);

        return setKeys(entity, contentProperty, keys);
    }
}
