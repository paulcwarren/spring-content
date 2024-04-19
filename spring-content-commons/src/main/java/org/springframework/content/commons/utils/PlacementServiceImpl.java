package org.springframework.content.commons.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.Set;

public class PlacementServiceImpl extends DefaultConversionService implements PlacementService {

    private static Log logger = LogFactory.getLog(PlacementServiceImpl.class);

    public static final String CONTENT_PROPERTY_INFO_GENERIC_PARAMETERS_MISSING_MESSAGE = "Unable to determine entity type <S> and content id type <SID> for " +
            ContentPropertyInfo.class.getName() + "; does the class parameterize those types?";

    public PlacementServiceImpl() {
        // Issue #57
        //
        // Remove the FallbackObjectToStringConverter (Object -> String).  This converter can cause issues with Entities
        // with String-arg Constructors.  Because the conversion service considers class hierarchies this converter will
        // match the canConvert(entity.getClass(), String.class) call in getResource(S entity) and be used (incorrectly)
        // to determine the entity's location.  Since there is no way to turn of the hierachy matching we remove this
        // converter instead forcing only matching on the domain object class -> String class.
        this.removeConvertible(Object.class, String.class);
    }

    // Duplicate of private org.springframework.core.convert.support.GenericConversionService::getRequiredTypeInfo
    @Nullable
    private ResolvableType[] getRequiredGenericParameters(Class<?> converterClass, Class<?> genericIfc) {
        ResolvableType resolvableType = ResolvableType.forClass(converterClass).as(genericIfc);
        ResolvableType[] generics = resolvableType.getGenerics();
        if (generics.length < 2) {
            return null;
        }
        Class<?> sourceType = generics[0].resolve();
        Class<?> targetType = generics[1].resolve();
        if (sourceType == null || targetType == null) {
            return null;
        }
        return generics;
    }

    @Override
    public void addConverter(Converter<?, ?> converter) {
        ResolvableType[] generics = getRequiredGenericParameters(converter.getClass(), Converter.class);
        if (generics == null) {
            throw new IllegalArgumentException("Unable to determine source type <S> and target type <T> for your " +
                    "Converter [" + converter.getClass().getName() + "]; does the class parameterize those types?");
        }
        ResolvableType sourceType = generics[0];
        logger.info("generics[0] sourceType: " + sourceType.toString());
        if (sourceType.resolve() == ContentPropertyInfo.class) {
            ResolvableType targetType = generics[1];
            logger.info("generics[1] targetType: " + targetType.toString());

            ResolvableType[] sourceTypeGenerics = sourceType.getGenerics();
            if (sourceTypeGenerics.length != 2) {
                throw new IllegalArgumentException(CONTENT_PROPERTY_INFO_GENERIC_PARAMETERS_MISSING_MESSAGE);
            }

            logger.info("sourceTypeGenerics[0]: " + sourceTypeGenerics[0].toString());
            logger.info("sourceTypeGenerics[1]: " + sourceTypeGenerics[1].toString());

            Class<?> entityClass = sourceTypeGenerics[0].resolve();
            Class<?> contentIdClass = sourceTypeGenerics[1].resolve();
            if (entityClass == null || contentIdClass == null) {
                throw new IllegalArgumentException(CONTENT_PROPERTY_INFO_GENERIC_PARAMETERS_MISSING_MESSAGE);
            }

            logger.info("Adding converter as ContentPropertyInfoConverterAdapter");
            addConverter(new ContentPropertyInfoConverterAdapter(converter, sourceType, targetType, entityClass, contentIdClass));
        } else {
            logger.info("Adding as regular converter");
            super.addConverter(converter);
        }
    }

    @Override
    public <S, T> void addConverter(Class<S> sourceType, Class<T> targetType, Converter<? super S, ? extends T> converter) {
        if (sourceType == ContentPropertyInfo.class) {
            throw new IllegalArgumentException("Adding of" + ContentPropertyInfo.class.getName() +
                    " converter is not supported by this method; use addConverter(Converter<?, ?> converter) instead");

            // add similar checks as for addConverter(Converter<?, ?> converter) if we decide to add support
//            ResolvableType sourceResolvableType = ResolvableType.forClass(sourceType).as(ContentPropertyInfo.class);
//
//            ResolvableType[] sourceTypeGenerics = sourceResolvableType.getGenerics();
//            Class<?> entityClass = sourceTypeGenerics[0].resolve();
//            Class<?> contentIdClass = sourceTypeGenerics[1].resolve();
//
//            addConverter(new ContentPropertyInfoConverterAdapter(converter, ResolvableType.forClass(sourceType), ResolvableType.forClass(targetType), entityClass, contentIdClass));
        } else {
            super.addConverter(sourceType, targetType, converter);
        }
    }

    public String toStringObject() {
        return PlacementServiceImpl.class.getName() + "@" + Integer.toHexString(hashCode());
    }

    /**
     * A {@link ConditionalGenericConverter} for {@link ContentPropertyInfo} that does conversion only if
     * generic parameter types of provided {@link ContentPropertyInfo} are compatible.
     */
    // Based on org.springframework.core.convert.support.GenericConversionService.ConverterAdapter.
    private final class ContentPropertyInfoConverterAdapter implements ConditionalGenericConverter {

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
            Assert.notNull(entityClass, "entityClass cannot be null");
            this.contentIdClass = contentIdClass;
            Assert.notNull(contentIdClass, "contentIdClass cannot be null");
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
            if (sourceEntityClass == null || !entityClass.isAssignableFrom(sourceEntityClass)){
                return false;
            }
            Class<?> sourceContentIdClass  = generics[1].resolve();
            if (sourceContentIdClass == null || !contentIdClass.isAssignableFrom(sourceContentIdClass)){
                return false;
            }

            return !(this.converter instanceof ConditionalConverter) ||
                    ((ConditionalConverter) this.converter).matches(sourceType, targetType);
        }

        @Override
        @Nullable
        public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            if (source == null) {
                return convertNullSource(sourceType, targetType);
            }
            return this.converter.convert(source);
        }

        @Override
        public String toString() {
            return (this.typeInfo + " : " + this.converter);
        }
    }
}