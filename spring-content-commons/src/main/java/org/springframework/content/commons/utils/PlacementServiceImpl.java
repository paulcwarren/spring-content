package org.springframework.content.commons.utils;

import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.Set;

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
        if (sourceType.resolve() == ContentPropertyInfo.class) {
            ResolvableType targetType = generics[1];

            ResolvableType[] sourceTypeGenerics = sourceType.getGenerics();
            if (sourceTypeGenerics.length != 2) {
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ConversionService converters =\n");
        for (String converterString : getConverterStrings()) {
            builder.append('\t').append(converterString).append('\n');
        }
        return builder.toString();
    }

    private List<String> getConverterStrings() {
        List<String> converterStrings = new ArrayList<>();
        for (ConvertersForPair convertersForPair : this.converters.converters.values()) {
            converterStrings.add(convertersForPair.toString());
        }
        Collections.sort(converterStrings);
        return converterStrings;
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


    // implementation of genericconversaionservice

    @Override
    public void addConverter(GenericConverter converter) {
        if (this.converters == null) {
            this.converters = new Converters();
        }

        this.converters.add(converter);
        invalidateCache();
    }

    private void invalidateCache() {
        if (this.converterCache == null) {
            this.converterCache = new ConcurrentReferenceHashMap<>(64);
        }
        this.converterCache.clear();
    }

    @Override
    @Nullable
    public <T> T convert(@Nullable Object source, Class<T> targetType) {
        Assert.notNull(targetType, "Target type to convert to cannot be null");
        return (T) convert(source, TypeDescriptor.forObject(source), TypeDescriptor.valueOf(targetType));
    }

    @Override
    @Nullable
    public Object convert(@Nullable Object source, @Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
        logger.info("Converting " + source + " of type " + sourceType + " to type " + targetType);

        Assert.notNull(targetType, "Target type to convert to cannot be null");
        if (sourceType == null) {
            Assert.isTrue(source == null, "Source must be [null] if source type == [null]");
            return handleResult(null, targetType, convertNullSource(null, targetType));
        }
        if (source != null && !sourceType.getObjectType().isInstance(source)) {
            throw new IllegalArgumentException("Source to convert from must be an instance of [" +
                    sourceType + "]; instead it was a [" + source.getClass().getName() + "]");
        }
        GenericConverter converter = getConverter(sourceType, targetType);
        if (converter != null) {
//            Object result = ConversionUtils.invokeConverter(converter, source, sourceType, targetType);
            Object result = null;
            try {
                logger.info("Calling converter " + converter);
                result = converter.convert(source, sourceType, targetType);
                logger.info("Got result " + result);
            }
            catch (ConversionFailedException ex) {
                throw ex;
            }
            catch (Throwable ex) {
                throw new ConversionFailedException(sourceType, targetType, source, ex);
            }

            logger.info("Returning result " + result);
            return handleResult(sourceType, targetType, result);
        }
        return handleConverterNotFound(source, sourceType, targetType);
    }

    @Nullable
    private Object handleResult(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType, @Nullable Object result) {
        if (result == null) {
            assertNotPrimitiveTargetType(sourceType, targetType);
        }
        return result;
    }

    private void assertNotPrimitiveTargetType(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (targetType.isPrimitive()) {
            throw new ConversionFailedException(sourceType, targetType, null,
                    new IllegalArgumentException("A null value cannot be assigned to a primitive type"));
        }
    }

    @Nullable
    private Object handleConverterNotFound(
            @Nullable Object source, @Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {

        if (source == null) {
            assertNotPrimitiveTargetType(sourceType, targetType);
            return null;
        }
        if ((sourceType == null || sourceType.isAssignableTo(targetType)) &&
                targetType.getObjectType().isInstance(source)) {
            return source;
        }
        throw new ConverterNotFoundException(sourceType, targetType);
    }

    @Nullable
    protected GenericConverter getConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
        logger.info("Getting converter for type " + sourceType + " to type " + targetType);

        ConverterCacheKey key = new ConverterCacheKey(sourceType, targetType);
        GenericConverter converter = this.converterCache.get(key);
        logger.info("Fetched from cache " + converter);
        if (converter != null) {
            return (converter != NO_MATCH ? converter : null);
        }

        converter = this.converters.find(sourceType, targetType);
        if (converter == null) {
            converter = getDefaultConverter(sourceType, targetType);
            logger.info("Got default converter " + converter);
        }

        if (converter != null) {
            logger.info("Adding converter " + converter + " to cache");
            this.converterCache.put(key, converter);
            logger.info("Returning converter " + converter);
            return converter;
        }

        this.converterCache.put(key, NO_MATCH);
        logger.info("Returning no_match converter");
        return null;
    }

    private static final class ConverterCacheKey implements Comparable<ConverterCacheKey> {

        private final TypeDescriptor sourceType;

        private final TypeDescriptor targetType;

        public ConverterCacheKey(TypeDescriptor sourceType, TypeDescriptor targetType) {
            this.sourceType = sourceType;
            this.targetType = targetType;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return (this == other || (other instanceof ConverterCacheKey that &&
                    this.sourceType.equals(that.sourceType)) &&
                    this.targetType.equals(that.targetType));
        }

        @Override
        public int hashCode() {
            return this.sourceType.hashCode() * 29 + this.targetType.hashCode();
        }

        @Override
        public String toString() {
            return "ConverterCacheKey [sourceType = " + this.sourceType + ", targetType = " + this.targetType + "]";
        }

        @Override
        public int compareTo(ConverterCacheKey other) {
            int result = this.sourceType.getResolvableType().toString().compareTo(
                    other.sourceType.getResolvableType().toString());
            if (result == 0) {
                result = this.targetType.getResolvableType().toString().compareTo(
                        other.targetType.getResolvableType().toString());
            }
            return result;
        }
    }

    private static class Converters {

        private final Set<GenericConverter> globalConverters = new CopyOnWriteArraySet<>();

        private final Map<GenericConverter.ConvertiblePair, ConvertersForPair> converters = new ConcurrentHashMap<>(256);

        public void add(GenericConverter converter) {
            Set<GenericConverter.ConvertiblePair> convertibleTypes = converter.getConvertibleTypes();
            if (convertibleTypes == null) {
                Assert.state(converter instanceof ConditionalConverter,
                        "Only conditional converters may return null convertible types");
                this.globalConverters.add(converter);
            }
            else {
                for (GenericConverter.ConvertiblePair convertiblePair : convertibleTypes) {
                    getMatchableConverters(convertiblePair).add(converter);
                }
            }
        }

        private ConvertersForPair getMatchableConverters(GenericConverter.ConvertiblePair convertiblePair) {
            return this.converters.computeIfAbsent(convertiblePair, k -> new ConvertersForPair());
        }

        public void remove(Class<?> sourceType, Class<?> targetType) {
            this.converters.remove(new GenericConverter.ConvertiblePair(sourceType, targetType));
        }

        /**
         * Find a {@link GenericConverter} given a source and target type.
         * <p>This method will attempt to match all possible converters by working
         * through the class and interface hierarchy of the types.
         * @param sourceType the source type
         * @param targetType the target type
         * @return a matching {@link GenericConverter}, or {@code null} if none found
         */
        @Nullable
        public GenericConverter find(TypeDescriptor sourceType, TypeDescriptor targetType) {
            logger.info("Finding converter for type " + sourceType + " to type " + targetType);

            // Search the full type hierarchy
            List<Class<?>> sourceCandidates = getClassHierarchy(sourceType.getType());
            List<Class<?>> targetCandidates = getClassHierarchy(targetType.getType());

            logger.info("Source candidates " + sourceCandidates);
            logger.info("Target candidates " + targetCandidates);

            for (Class<?> sourceCandidate : sourceCandidates) {
                for (Class<?> targetCandidate : targetCandidates) {
                    GenericConverter.ConvertiblePair convertiblePair = new GenericConverter.ConvertiblePair(sourceCandidate, targetCandidate);
                    GenericConverter converter = getRegisteredConverter(sourceType, targetType, convertiblePair);
                    if (converter != null) {
                        logger.info("Found converter " + converter);
                        return converter;
                    }
                }
            }
            logger.info("No converter found");
            return null;
        }

        @Nullable
        private GenericConverter getRegisteredConverter(TypeDescriptor sourceType,
                                                        TypeDescriptor targetType, GenericConverter.ConvertiblePair convertiblePair) {

            // Check specifically registered converters
            ConvertersForPair convertersForPair = this.converters.get(convertiblePair);
            if (convertersForPair != null) {
                GenericConverter converter = convertersForPair.getConverter(sourceType, targetType);
                if (converter != null) {
                    return converter;
                }
            }
            // Check ConditionalConverters for a dynamic match
            for (GenericConverter globalConverter : this.globalConverters) {
                if (((ConditionalConverter) globalConverter).matches(sourceType, targetType)) {
                    return globalConverter;
                }
            }
            return null;
        }

        /**
         * Returns an ordered class hierarchy for the given type.
         * @param type the type
         * @return an ordered list of all classes that the given type extends or implements
         */
        private List<Class<?>> getClassHierarchy(Class<?> type) {
            List<Class<?>> hierarchy = new ArrayList<>(20);
            Set<Class<?>> visited = new HashSet<>(20);
            addToClassHierarchy(0, ClassUtils.resolvePrimitiveIfNecessary(type), false, hierarchy, visited);
            boolean array = type.isArray();

            int i = 0;
            while (i < hierarchy.size()) {
                Class<?> candidate = hierarchy.get(i);
                candidate = (array ? candidate.componentType() : ClassUtils.resolvePrimitiveIfNecessary(candidate));
                Class<?> superclass = candidate.getSuperclass();
                if (superclass != null && superclass != Object.class && superclass != Enum.class) {
                    addToClassHierarchy(i + 1, candidate.getSuperclass(), array, hierarchy, visited);
                }
                addInterfacesToClassHierarchy(candidate, array, hierarchy, visited);
                i++;
            }

            if (Enum.class.isAssignableFrom(type)) {
                addToClassHierarchy(hierarchy.size(), Enum.class, array, hierarchy, visited);
                addToClassHierarchy(hierarchy.size(), Enum.class, false, hierarchy, visited);
                addInterfacesToClassHierarchy(Enum.class, array, hierarchy, visited);
            }

            addToClassHierarchy(hierarchy.size(), Object.class, array, hierarchy, visited);
            addToClassHierarchy(hierarchy.size(), Object.class, false, hierarchy, visited);
            return hierarchy;
        }

        private void addInterfacesToClassHierarchy(Class<?> type, boolean asArray,
                                                   List<Class<?>> hierarchy, Set<Class<?>> visited) {

            for (Class<?> implementedInterface : type.getInterfaces()) {
                addToClassHierarchy(hierarchy.size(), implementedInterface, asArray, hierarchy, visited);
            }
        }

        private void addToClassHierarchy(int index, Class<?> type, boolean asArray,
                                         List<Class<?>> hierarchy, Set<Class<?>> visited) {

            if (asArray) {
                type = type.arrayType();
            }
            if (visited.add(type)) {
                hierarchy.add(index, type);
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ConversionService converters =\n");
            for (String converterString : getConverterStrings()) {
                builder.append('\t').append(converterString).append('\n');
            }
            return builder.toString();
        }

        private List<String> getConverterStrings() {
            List<String> converterStrings = new ArrayList<>();
            for (ConvertersForPair convertersForPair : this.converters.values()) {
                converterStrings.add(convertersForPair.toString());
            }
            Collections.sort(converterStrings);
            return converterStrings;
        }
    }

    private static class ConvertersForPair {

        private final Deque<GenericConverter> converters = new ConcurrentLinkedDeque<>();

        public void add(GenericConverter converter) {
            this.converters.addFirst(converter);
        }

        @Nullable
        public GenericConverter getConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
            for (GenericConverter converter : this.converters) {
                if (!(converter instanceof ConditionalGenericConverter genericConverter) ||
                        genericConverter.matches(sourceType, targetType)) {
                    return converter;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return StringUtils.collectionToCommaDelimitedString(this.converters);
        }
    }

    private static class NoOpConverter implements GenericConverter {

        private final String name;

        public NoOpConverter(String name) {
            this.name = name;
        }

        @Override
        @Nullable
        public Set<ConvertiblePair> getConvertibleTypes() {
            return null;
        }

        @Override
        @Nullable
        public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            return source;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

}

