package org.springframework.content.commons.storeservice;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public interface Stores {
    StoreFilter MATCH_ALL = info -> true;

    static StoreFilter withDomainClass(Class<?> domainClass) {
        Assert.notNull(domainClass);
        return info -> domainClass.equals(info.getDomainObjectClass());
    }

    StoreInfo getStore(Class<?> storeType, StoreFilter filter);

    StoreInfo[] getStores(Class<?> storeType);

    StoreInfo[] getStores(Class<?> storeType, StoreFilter filter);
}
