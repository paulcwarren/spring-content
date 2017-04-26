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
import org.springframework.content.commons.repository.ContentStore;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class )
@Ginkgo4jConfiguration(threads=1)
public class AbstractStoreFactoryBeanTest {

    {
        Describe("AbstractContentStoreFactoryBean", () -> {

        	Context("#getDomainClass", () -> {
                It("gets the domain class", () -> {
                    TestContentStoreFactory factory = new TestContentStoreFactory();
                    Class<?> domainClass = factory.getDomainClass(TestStore.class);
                    assertThat(domainClass, is(equalTo(String.class)));
                });
                It("when ContentStore isn't the first extended interface it still get the domain type", () -> {
                    TestContentStoreFactory factory = new TestContentStoreFactory();
                    Class<?> domainClass = factory.getDomainClass(ContentStoreNotFirstIntefaceStore.class);
                    assertThat(domainClass, is(equalTo(String.class)));
                });
            });

            Context("#getContentIdClass", () -> {
                It("gets the domain id", () -> {
                    TestContentStoreFactory factory = new TestContentStoreFactory();
                    Class<? extends Serializable> domainId = factory.getContentIdClass(TestStore.class);
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

    public static class TestConfigStoreImpl implements ContentStore<Object,Serializable> {

		@Override public void setContent(Object property, InputStream content) {}

		@Override public void unsetContent(Object property) {}

		@Override public InputStream getContent(Object property) { return null; }

    }

    public interface TestStore extends ContentStore<String, UUID> {
    }

    public interface ContentStoreNotFirstIntefaceStore extends Serializable, ContentStore<String, UUID> {
    }
}
