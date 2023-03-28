package org.springframework.content.commons.utils;

import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.ReactiveContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.Pair;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public final class StoreInterfaceUtils {
    private StoreInterfaceUtils() {}

    public static Pair<Optional<Class<?>>, Class<? extends Serializable>> getStoreTypes(Class<? extends org.springframework.content.commons.repository.Store> storeClass) {
        List<TypeInformation<?>> types = null;

        Class<?>[] candidateStoreClasses = new Class<?>[]{
                ContentStore.class,
                AssociativeStore.class,
                Store.class,
                ReactiveContentStore.class,
                org.springframework.content.commons.repository.ContentStore.class,
                org.springframework.content.commons.repository.AssociativeStore.class,
                org.springframework.content.commons.repository.Store.class,
                org.springframework.content.commons.repository.ReactiveContentStore.class
        };
        for (Class<?> candidateStoreClass : candidateStoreClasses) {
            try {
                types = ClassTypeInformation.from(storeClass).getRequiredSuperTypeInformation(candidateStoreClass).getTypeArguments();
                break;
            } catch (IllegalArgumentException iae) {
            }
        }

        Optional<Class<?>> domainClass = null;
        Class<? extends Serializable> idClass = null;

        if (types.size() == 2) {
            domainClass = Optional.of(types.get(0).getType());
            Assert.isAssignable(Serializable.class, types.get(1).getType());
            idClass = (Class<? extends Serializable>) types.get(1).getType();
        } else if (types.size() == 1) {
            domainClass = Optional.empty();
            Assert.isAssignable(Serializable.class, types.get(0).getType());
            idClass = (Class<? extends Serializable>) types.get(0).getType();
        }

        return Pair.of(domainClass, idClass);
    }
}
