package org.springframework.content.test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;

public class TestStoreFactoryBean extends AbstractStoreFactoryBean {

	@Override
	protected Object getContentStoreImpl() {
		return new TestConfigStoreImpl();
	}

	public static class TestConfigStoreImpl implements ContentStore {
		@Override
		public Object setContent(Object property, InputStream content) {
			return null;
		}

		@Override
		public Object unsetContent(Object property) {
			return null;
		}

		@Override
		public InputStream getContent(Object property) {
			return new ByteArrayInputStream("some test content".getBytes());
		}
	}
}
