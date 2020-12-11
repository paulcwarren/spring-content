package org.springframework.content.commons.store;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.UUID;

import org.springframework.content.commons.annotations.GenericGenerator;
import org.springframework.content.commons.utils.BeanUtils;

public class ContentIdGeneratorManager {

    @SuppressWarnings("unchecked")
    public ValueGenerator<Object,Serializable> generator(Class<?> entityClass)
            throws InstantiationException, IllegalAccessException {

        ValueGenerator<Object,Serializable> generator = null;

        Field generatorField = BeanUtils.findFieldWithAnnotation(entityClass, GenericGenerator.class);

        if (generatorField != null) {
            GenericGenerator generatorMetadata = generatorField.getAnnotation(GenericGenerator.class);

            Class<?> generatorClass = generatorMetadata.strategy();
            generator = (ValueGenerator<Object,Serializable>) generatorClass.newInstance();
        } else {

            generator = new ValueGenerator<Object,Serializable>() {

                @Override
                public Serializable generate(Object entity) {
                    return UUID.randomUUID();
                }
            };
        }

        return generator;
    }
}
