package org.springframework.content.commons.repository.factory;

import java.io.Serializable;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import lombok.Getter;
import lombok.Setter;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.factory.testsupport.EnableTestStores;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads = 1)
@ContextConfiguration(classes = StoreFragmentTest.StoreTestConfiguration.class)
public class StoreFragmentTest {

	@Autowired
	private ApplicationContext context;

	{
		Describe("given a store definition", () -> {

			Context("given the application context", () -> {

				It("should support the extension", () -> {

					assertThat(context.getBean(TestContentStore.class), is(not(nullValue())));
					assertThat(context.getBean(CustomizationImpl.class).getBean(), is("Spring Content"));
					assertThat(context.getBean(CustomizationImpl.class).getDomainClass(), is(Object.class));
					assertThat(context.getBean(CustomizationImpl.class).getIdClass(), is(Serializable.class));
					assertThat(context.getBean(TestContentStore.class).greet("World"), is("Hello Spring Content World"));
				});
			});
		});
	}

	@Configuration
	@EnableTestStores
	public static class StoreTestConfiguration {

		@Bean
		public String bean() {
			return "Spring Content";
		}
	}

	public interface TestContentStore extends ContentStore<Object, Serializable>, Customization {
	}

	public interface Customization {
		String greet(String name);
	}

	@Getter
	@Setter
	public static class CustomizationImpl implements Customization {

		@Autowired
		private String bean;

		private Class<?> domainClass;
		private Class<?> idClass;

		@Override
		public String greet(String name) {
			return "Hello " + bean + " " + name;
		}
	}

	@Test
	public void noop() {
	}
}
