package internal.org.springframework.content.encryption.keys.converter;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

@RequiredArgsConstructor
public class EncryptedDataEncryptionKeyGenericConverter implements ConditionalGenericConverter {

    private final ConversionService conversionService;

    private static final TypeDescriptor ENCRYPTED_DEK = TypeDescriptor.valueOf(StoredDataEncryptionKey.class);
    private static final Collection<TypeDescriptor> SUB_TYPES = permittedSubClasses(StoredDataEncryptionKey.class).stream()
            .map(TypeDescriptor::valueOf)
            .toList();

    private static Collection<Class<?>> permittedSubClasses(Class<?> type) {
        var permittedSubclasses = type.getPermittedSubclasses();
        if(permittedSubclasses == null) {
            return Set.of(type);
        }
        var permitted = new LinkedHashSet<Class<?>>(permittedSubclasses.length);
        for (var subClass : permittedSubclasses) {
            permitted.addAll(permittedSubClasses(subClass));
        }
        return Collections.unmodifiableCollection(permitted);
    }

    private static Stream<TypeDescriptor> typesAssignableTo(TypeDescriptor typeDescriptor) {
        return SUB_TYPES.stream()
                .filter(type -> type.isAssignableTo(typeDescriptor));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        if(targetType.isAssignableTo(ENCRYPTED_DEK)) {
            return typesAssignableTo(targetType)
                    .anyMatch(type -> conversionService.canConvert(sourceType, type));
        } else if(sourceType.isAssignableTo(ENCRYPTED_DEK)) {
            return typesAssignableTo(targetType)
                    .anyMatch(type -> conversionService.canConvert(type, targetType));
        }
        return false;

    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return null;
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if(targetType.isAssignableTo(ENCRYPTED_DEK)) {
            return typesAssignableTo(targetType)
                    .map(type -> conversionService.convert(source, sourceType, type))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        } else if(sourceType.isAssignableTo(ENCRYPTED_DEK)) {
            return typesAssignableTo(sourceType)
                    .map(type -> conversionService.convert(source, type, targetType))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}
