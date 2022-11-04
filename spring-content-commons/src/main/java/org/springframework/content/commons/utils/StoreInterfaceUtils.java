package org.springframework.content.commons.utils;

import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.ReactiveContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.Pair;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public final class StoreInterfaceUtils {
    private StoreInterfaceUtils() {}

    public static Pair<Optional<Class<?>>, Class<? extends Serializable>> getStoreTypes(Class<? extends Store> storeClass) {
        List<TypeInformation<?>> types = null;
        try {
            types = ClassTypeInformation.from(storeClass).getRequiredSuperTypeInformation(ContentStore.class).getTypeArguments();
        } catch (IllegalArgumentException iae) {
            try {
                types = ClassTypeInformation.from(storeClass).getRequiredSuperTypeInformation(AssociativeStore.class).getTypeArguments();
            } catch (IllegalArgumentException iae2) {
                try {
                    types = ClassTypeInformation.from(storeClass).getRequiredSuperTypeInformation(Store.class).getTypeArguments();
                } catch (IllegalArgumentException iae3) {
                    try {
                        types = ClassTypeInformation.from(storeClass).getRequiredSuperTypeInformation(ReactiveContentStore.class).getTypeArguments();
                    } catch (IllegalArgumentException iae4) {
                    }
                }
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
