package org.springframework.content.commons.repository.factory.testsupport;

import java.io.InputStream;
import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;
import org.springframework.core.io.Resource;

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
		public Object setContent(Object property, Resource resourceContent) {
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

        @Override
        public Resource getResource(Object entity, PropertyPath propertyPath) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void associate(Object entity, PropertyPath propertyPath, Serializable id) {
            // TODO Auto-generated method stub

        }

        @Override
        public void unassociate(Object entity, PropertyPath propertyPath) {
            // TODO Auto-generated method stub

        }

        @Override
        public Object setContent(Object property, PropertyPath propertyPath, InputStream content) {
            // TODO Auto-generated method stub
            return null;
        }

		@Override
		public Object setContent(Object property, PropertyPath propertyPath, InputStream content, long contentLen) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
        public Object setContent(Object property, PropertyPath propertyPath, Resource resourceContent) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object unsetContent(Object property, PropertyPath propertyPath) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public InputStream getContent(Object property, PropertyPath propertyPath) {
            // TODO Auto-generated method stub
            return null;
        }
	}
}
