package org.springframework.content.commons.mappingcontext;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * A {@link ConditionalGenericConverter} that does conversion only if generic parameter types of {@link ContentPropertyInfo} match.
 */
// Inspired by GenericConversionService.ConverterAdapter.
public class ContentPropertyInfoConverterAdapter implements ConditionalGenericConverter {

    private final Converter<Object, Object> converter;

    private final ConvertiblePair typeInfo;

    private final ResolvableType targetType;

    private final Class<?> entityClass;
    private final Class<?> contentIdClass;

    public ContentPropertyInfoConverterAdapter(Converter<?, ?> converter, ResolvableType sourceType, ResolvableType targetType, Class<?> entityClass, Class<?> contentIdClass) {
        this.converter = (Converter<Object, Object>) converter;
        this.typeInfo = new ConvertiblePair(sourceType.toClass(), targetType.toClass());
        this.targetType = targetType;
        this.entityClass = entityClass;
        this.contentIdClass = contentIdClass;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(this.typeInfo);
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        // targetType checks (same as in GenericConversionService.ConverterAdapter)
        // Check raw type first...
        if (this.typeInfo.getTargetType() != targetType.getObjectType()) {
            return false;
        }
        // Full check for complex generic type match required?
        ResolvableType rt = targetType.getResolvableType();
        if (!(rt.getType() instanceof Class) && !rt.isAssignableFrom(this.targetType) &&
                !this.targetType.hasUnresolvableGenerics()) {
            return false;
        }

        // sourceType (ContentPropertyInfo) checks
        ResolvableType[] generics = sourceType.getResolvableType().getGenerics();
        Class<?> sourceEntityClass  = generics[0].resolve();
        if (!entityClass.isAssignableFrom(sourceEntityClass)){
            return false;
        }
        Class<?> sourceContentIdClass  = generics[1].resolve();
        if (!contentIdClass.isAssignableFrom(sourceContentIdClass)){
            return false;
        }
        // return true;

        return !(this.converter instanceof ConditionalConverter) ||
                ((ConditionalConverter) this.converter).matches(sourceType, targetType);
    }

    @Override
    @Nullable
    public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
//        if (source == null) {
//            return convertNullSource(sourceType, targetType);
//        }
        return this.converter.convert(source);
    }

    @Override
    public String toString() {
        return (this.typeInfo + " : " + this.converter);
    }
}
