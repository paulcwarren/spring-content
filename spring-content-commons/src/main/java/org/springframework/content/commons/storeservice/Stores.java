package org.springframework.content.commons.storeservice;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public interface Stores {
    StoreFilter MATCH_ALL = new StoreFilter() {
        @Override
        public String name() {
            return "MATCH_ALL";
        }
        @Override
        public boolean matches(StoreInfo info) {
            return true;
        }
    };

    static StoreFilter withDomainClass(Class<?> domainClass) {
        Assert.notNull(domainClass, "domainClass must not be null");

        return new StoreFilter() {
            @Override
            public String name() {
                return domainClass.getCanonicalName();
            }
            @Override
            public boolean matches(StoreInfo info) {
                return domainClass.equals(info.getDomainObjectClass());
            }
        };
    }

    void addStoreResolver(String name, StoreResolver resolver);

    StoreInfo getStore(Class<?> storeType, StoreFilter filter);

    StoreInfo[] getStores(StoreFilter filter);

    StoreInfo[] getStores(Class<?> storeType);

    StoreInfo[] getStores(Class<?> storeType, StoreFilter filter);
}
