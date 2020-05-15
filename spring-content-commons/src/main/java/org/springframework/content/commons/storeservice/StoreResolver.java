package org.springframework.content.commons.storeservice;

public interface StoreResolver {
    StoreInfo resolve(StoreInfo... stores);
}
