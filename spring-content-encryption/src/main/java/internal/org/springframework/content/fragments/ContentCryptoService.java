package internal.org.springframework.content.fragments;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.content.commons.io.RangeableResource;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.encryption.engine.DataEncryptionEngine;
import org.springframework.content.encryption.engine.DataEncryptionEngine.EncryptionParameters;
import org.springframework.content.encryption.engine.DataEncryptionEngine.InputStreamRequestParameters;
import org.springframework.content.encryption.keys.DataEncryptionKeyAccessor;
import org.springframework.content.encryption.keys.DataEncryptionKeyEncryptor;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Encryption logic in support of {@link EncryptingContentStoreImpl}
 *
 * @param <S> Type of the entity
 * @param <DEK> Type of the encrypted data encryption key
 */
@RequiredArgsConstructor
class ContentCryptoService<S, DEK extends StoredDataEncryptionKey> {

    private final MappingContext mappingContext;
    private final DataEncryptionKeyAccessor<S, DEK> dataEncryptionKeyAccessor;
    private final List<DataEncryptionKeyEncryptor<DEK>> dataEncryptionKeyEncryptors;
    private final DataEncryptionEngine encryptionEngine;

    public S encrypt(S entity, PropertyPath propertyPath, InputStream plainText, BiFunction<S, InputStream, S> contentSetter) {
        Assert.notNull(entity, "entity not set");
        Assert.notNull(propertyPath, "propertyPath not set");
        Assert.notNull(plainText, "plainText not set");

        var contentProperty = resolveContentPropertyRequired(entity, propertyPath);

        var encryptionParameters = encryptionEngine.createNewParameters();
        var encryptedDeks = dataEncryptionKeyEncryptors.stream()
                .map(p -> p.wrapEncryptionKey(encryptionParameters))
                .toList();
        var newEntity = dataEncryptionKeyAccessor.setKeys(entity, contentProperty, encryptedDeks);
        var encryptedStream = encryptionEngine.encrypt(plainText, encryptionParameters);

        return contentSetter.apply(newEntity, encryptedStream);
    }

    public Resource decrypt(S entity, PropertyPath propertyPath, GetResourceParams getResourceParams, Supplier<Resource> contentGetter) {
        Assert.notNull(entity, "entity not set");
        Assert.notNull(propertyPath, "propertyPath not set");

        var contentProperty = resolveContentPropertyRequired(entity, propertyPath);

        var encryptedDeks = dataEncryptionKeyAccessor.findKeys(entity, contentProperty);
        var resource = contentGetter.get();
        if(encryptedDeks == null) {
            // Content is not encrypted; return the original resource
            return resource;
        }
        var encryptionParameters = decryptEncryptionParameters(encryptedDeks);
        if (encryptionParameters == null) {
            throw new StoreAccessException(String.format("Content property %s can not be decrypted".formatted(propertyPath.getName())));
        }


        InputStreamRequestParameters requestParams = InputStreamRequestParameters.full();
        try {
            if(getResourceParams != null) {
                requestParams = parseRangePattern(getResourceParams.getRange(), resource);
            }
        } catch(IOException ex) {
            throw new StoreAccessException(String.format("Content property %s can not be accessed".formatted(propertyPath.getName())), ex);
        }

        InputStreamRequestParameters finalRequestParams = requestParams;

        return new DecryptedResource(() -> {
            return encryptionEngine.decrypt(params -> {
                if (resource instanceof RangeableResource rr) {
                    rr.setRange(constructRangePattern(params));
                }
                try {
                    return resource.getInputStream();
                } catch (IOException ex) {
                    throw new StoreAccessException(String.format("Content property %s can not be accessed".formatted(propertyPath.getName())), ex);
                }
            }, encryptionParameters, finalRequestParams);
        }, resource);

    }

    public S clearKeys(S entity, PropertyPath propertyPath) {
        return dataEncryptionKeyAccessor.clearKeys(entity, resolveContentPropertyRequired(entity, propertyPath));
    }

    private ContentProperty resolveContentPropertyRequired(S entity, PropertyPath propertyPath) {
        ContentProperty contentProperty = mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (contentProperty == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }
        return contentProperty;
    }

    private EncryptionParameters decryptEncryptionParameters(Collection<DEK> encryptedDeks) {
        for (var dataEncryptionKeyEncryptor : dataEncryptionKeyEncryptors) {
            for (var encryptedDek : encryptedDeks) {
                if(dataEncryptionKeyEncryptor.supports(encryptedDek)) {
                    return dataEncryptionKeyEncryptor.unwrapEncryptionKey(encryptedDek);
                }
            }
        }
        return null;
    }

    // As per https://www.rfc-editor.org/rfc/rfc9110.html#name-range; single-range support only
    private static final Pattern RANGE_PATTERN = Pattern.compile("\\Abytes=(?<firstPos>[0-9]*)-(?<lastPos>[0-9]*)\\Z");

    private static InputStreamRequestParameters parseRangePattern(String range, Resource resource)
            throws IOException {
        if(range == null || range.isEmpty()) {
            return InputStreamRequestParameters.full();
        }
        var matcher = RANGE_PATTERN.matcher(range);
        if(matcher.matches()) {
            var firstPosStr = matcher.group("firstPos");
            var lastPosStr = matcher.group("lastPos");
            if(firstPosStr.isEmpty() && lastPosStr.isEmpty()) {
                return InputStreamRequestParameters.full();
            } else if(firstPosStr.isEmpty()) {
                var contentLength = resource.contentLength();
                return InputStreamRequestParameters.startingFrom(contentLength - Long.parseUnsignedLong(lastPosStr));
            } else {
                return new InputStreamRequestParameters(
                        Long.parseUnsignedLong(firstPosStr),
                        lastPosStr.isEmpty()?null:Long.parseUnsignedLong(lastPosStr)
                );
            }
        } else {
            throw new StoreAccessException(String.format("Range request '%s' is not supported. Only a single byte-range is supported".formatted(range)));
        }
    }

    private static String constructRangePattern(InputStreamRequestParameters parameters) {
        return "bytes="+parameters.getStartByteOffset()+"-"+ Optional.ofNullable(parameters.getEndByteOffset()).map(Long::toUnsignedString).orElse("");
    }
}
