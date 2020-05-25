package org.springframework.content.commons.repository.factory.testsupport;

import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.io.Serializable;

public class TestStoreFactoryBean extends AbstractStoreFactoryBean {

	@Override
	protected Object getContentStoreImpl() {
		return new TestConfigStoreImpl();
	}

	public static class TestConfigStoreImpl implements ContentStore {
		@Override
		public Object setContent(Object property, InputStream content) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object unsetContent(Object property) {
			throw new UnsupportedOperationException();
		}

		@Override
		public InputStream getContent(Object property) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Resource getResource(Object entity) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void associate(Object entity, Serializable id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void unassociate(Object entity) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Resource getResource(Serializable id) {
			throw new UnsupportedOperationException();
		}
	}
}
