package org.springframework.content.commons.repository.factory;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.InputStream;
import java.io.Serializable;
import java.util.UUID;

import org.junit.runner.RunWith;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.core.io.Resource;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class AbstractStoreFactoryBeanTest {

	{
		Describe("AbstractContentStoreFactoryBean", () -> {

			Context("#getDomainClass", () -> {
				It("gets the domain class", () -> {
					TestContentStoreFactory factory = new TestContentStoreFactory();
					Class<?> domainClass = factory.getDomainClass(TestStore.class);
					assertThat(domainClass, is(equalTo(String.class)));
				});
				It("when ContentStore isn't the first extended interface it still get the domain type",
						() -> {
							TestContentStoreFactory factory = new TestContentStoreFactory();
							Class<?> domainClass = factory.getDomainClass(
									ContentStoreNotFirstIntefaceStore.class);
							assertThat(domainClass, is(equalTo(String.class)));
						});
			});

			Context("#getContentIdClass", () -> {
				It("gets the domain id", () -> {
					TestContentStoreFactory factory = new TestContentStoreFactory();
					Class<? extends Serializable> domainId = factory
							.getContentIdClass(TestStore.class);
					assertThat(domainId, is(equalTo(UUID.class)));
				});
			});
		});
	}

	public static class TestContentStoreFactory extends AbstractStoreFactoryBean {
		@Override
		protected Object getContentStoreImpl() {
			return new TestConfigStoreImpl();
		}
	}

	public static class TestConfigStoreImpl
			implements ContentStore<Object, Serializable> {

		@Override
		public Object setContent(Object property, InputStream content) {
			return null;
		}

		@Override
		public Object setContent(Object property, Resource resourceContent) {
			return null;
		}

		@Override
		public Object unsetContent(Object property) {
			return null;
		}

		@Override
		public InputStream getContent(Object property) {
			return null;
		}

		@Override
		public Resource getResource(Object entity) {
			return null;
		}

		@Override
		public void associate(Object entity, Serializable id) {

		}

		@Override
		public void unassociate(Object entity) {

		}

		@Override
		public Resource getResource(Serializable id) {
			return null;
		}

        @Override
        public Resource getResource(Object entity, PropertyPath propertyPath) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void associate(Object entity, Resource resource, PropertyPath propertyPath) {
            // TODO Auto-generated method stub

        }

        @Override
        public void unassociate(Object entity, PropertyPath propertyPath) {
            // TODO Auto-generated method stub

        }
	}

	public interface TestStore extends ContentStore<String, UUID> {
	}

	public interface ContentStoreNotFirstIntefaceStore
			extends Serializable, ContentStore<String, UUID> {
	}
}
