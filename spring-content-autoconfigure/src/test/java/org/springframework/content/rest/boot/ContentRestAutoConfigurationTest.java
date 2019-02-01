package org.springframework.content.rest.boot;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.rest.boot.autoconfigure.ContentRestAutoConfiguration;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.content.commons.annotations.Content;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.net.URI;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class ContentRestAutoConfigurationTest {

	{
		Describe("ContentRestAutoConfiguration", () -> {
			Context("given a default configuration", () -> {
				It("should load the context", () -> {
					AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
					context.register(TestConfig.class);
					context.setServletContext(new MockServletContext());
					context.refresh();

					assertThat(context.getBean("contentHandlerMapping"), is(not(nullValue())));
					assertThat(context.getBean("contentLinksProcessor"), is(not(nullValue())));

					context.close();
				});
			});

			Context("given an environment specifying a base uri", () -> {
				BeforeEach(() -> {
					System.setProperty("spring.content.rest.base-uri", "/contentApi");
				});
				AfterEach(() -> {
					System.clearProperty("spring.content.rest.base-uri");
				});
				It("should have a filesystem properties bean with the correct root set", () -> {
					AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
					context.register(TestConfig.class);
					context.setServletContext(new MockServletContext());
					context.refresh();

					assertThat(context.getBean(
							ContentRestAutoConfiguration.ContentRestProperties.class).getBaseUri(),
								is(URI.create("/contentApi")));

					context.close();
				});
			});
		});
	}

	@Configuration
	@AutoConfigurationPackage
	@EnableAutoConfiguration
	public static class TestConfig {
	}

	@Document
	@Content
	public class TestEntity {
		@Id
		private String id;
		@ContentId
		private String contentId;
	}

	public interface TestEntityRepository extends MongoRepository<TestEntity, String> {
	}

	public interface TestEntityContentRepository
			extends ContentStore<TestEntity, String> {
	}
}
