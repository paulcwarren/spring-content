package org.springframework.content.commons.repository.factory.testsupport;

import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;

public class TestStoreFactoryBean extends AbstractStoreFactoryBean {

	@Override
    protected Object getContentStoreImpl() {
        return new TestConfigStoreImpl();
    }

    public static class TestConfigStoreImpl {
    }
}
