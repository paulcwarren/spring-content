package org.springframework.content.commons.repository.factory.testsupport;

import org.springframework.content.commons.repository.factory.AbstractContentStoreFactoryBean;

public class TestStoreFactoryBean extends AbstractContentStoreFactoryBean {

	@Override
    protected Object getContentStoreImpl() {
        return new TestConfigStoreImpl();
    }

    public static class TestConfigStoreImpl {
    }
}
