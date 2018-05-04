package org.springframework.content.s3.config;

import org.springframework.content.s3.S3ObjectIdResolver;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class S3ObjectIdResolvers extends ArrayList<S3ObjectIdResolver> {

    public S3ObjectIdResolver getResolverFor(Class<?> idOrEntityType) {
        if (idOrEntityType == null)
            return null;

        for (S3ObjectIdResolver candidate : this) {
            Type[] types = candidate.getClass().getGenericInterfaces();

            for ( Type t : types ) {
                if (t instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) t;
                    types = pt.getActualTypeArguments();
                    if (types.length == 1) {
                        if (types[0].equals(idOrEntityType)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

}
