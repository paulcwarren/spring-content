package org.springframework.content.commons.utils;

import org.springframework.content.commons.mappingcontext.ContentPropertyInfo;
import org.springframework.content.commons.mappingcontext.ContentPropertyInfoConverterAdapter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;

public class PlacementServiceImpl extends DefaultConversionService implements PlacementService {

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

    @Override
    public void addConverter(Converter<?, ?> converter) {
        // logic is similar to GenericConversionService.getRequiredTypeInfo
        ResolvableType resolvableType = ResolvableType.forClass(converter.getClass()).as(Converter.class);
        ResolvableType[] generics = resolvableType.getGenerics();
        ResolvableType sourceType = generics[0];
        if (sourceType.resolve() == ContentPropertyInfo.class) {
            ResolvableType targetType = generics[1];

            ResolvableType[] sourceTypeGenerics = sourceType.getGenerics();
            if (generics.length != 2) {
                throw new IllegalArgumentException(CONTENT_PROPERTY_INFO_GENERIC_PARAMETERS_MISSING_MESSAGE);
            }

            Class<?> entityClass = sourceTypeGenerics[0].resolve();
            Class<?> contentIdClass = sourceTypeGenerics[1].resolve();
            if (entityClass == null || contentIdClass == null) {
                throw new IllegalArgumentException(CONTENT_PROPERTY_INFO_GENERIC_PARAMETERS_MISSING_MESSAGE);
            }

            addConverter(new ContentPropertyInfoConverterAdapter(converter, sourceType, targetType, entityClass, contentIdClass));
        } else {
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
}
