package org.springframework.content.test;

import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;

public class TestContentStoreFactory extends AbstractStoreFactoryBean {
    @Override
    protected Object getContentStoreImpl() {
        return new TestStoreFactoryBean.TestConfigStoreImpl();
    }
}

