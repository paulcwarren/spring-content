package internal.org.springframework.versions.jpa;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ClassUtils;
import org.springframework.versions.LockingAndVersioningException;

import java.lang.reflect.Constructor;

import static java.lang.String.format;

public class JpaCloningServiceImpl implements CloningService {

    private static Log logger = LogFactory.getLog(JpaCloningServiceImpl.class);

    @Override
    public Object clone(Object entity) {

        Class clazz = entity.getClass();
        Constructor copyCtor = ClassUtils.getConstructorIfAvailable(clazz, clazz);
        if (copyCtor == null) {
            throw new LockingAndVersioningException(format("no copy constructor: %s", clazz.getCanonicalName()));
        }

        Object newInstance = null;
        try {
            newInstance = copyCtor.newInstance(entity);
        } catch (Exception e) {
            throw new LockingAndVersioningException("copy constructor failed", e);
        }
        return newInstance;
    }

}
