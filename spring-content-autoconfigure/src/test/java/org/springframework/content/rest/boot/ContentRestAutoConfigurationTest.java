package org.springframework.content.rest.boot;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.rest.boot.autoconfigure.ContentRestAutoConfiguration;
import internal.org.springframework.content.rest.boot.autoconfigure.HypermediaAutoConfiguration;
import internal.org.springframework.content.rest.boot.autoconfigure.SpringBootContentRestConfigurer;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.net.URI;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class ContentRestAutoConfigurationTest {

	{
		Describe("ContentRestAutoConfiguration", () -> {
			Context("given a default configuration", () -> {
				It("should load the context", () -> {
					AnnotationConfigServletWebApplicationContext context = new AnnotationConfigServletWebApplicationContext();
					context.setServletContext(new MockServletContext());
					context.register(TestConfig.class, HypermediaConfig.class);
					context.refresh();

					assertThat(context.getBean("contentHandlerMapping"), is(not(nullValue())));
					assertThat(context.getBean("contentLinksProcessor"), is(not(nullValue())));

					context.close();
				});
			});

			Context("given an environment specifying rest properties", () -> {
				BeforeEach(() -> {
					System.setProperty("spring.content.rest.base-uri", "/contentApi");
					System.setProperty("spring.content.rest.fully-qualified-links", "false");
                    System.setProperty("spring.content.rest.shortcut-request-mappings.disabled", "true");
					System.setProperty("spring.content.rest.shortcut-request-mappings.excludes", "GET=a/b,c/d:PUT=*/*");
					System.setProperty("spring.content.rest.overwrite-existing-content", "false");
				});
				AfterEach(() -> {
					System.clearProperty("spring.content.rest.base-uri");
                    System.clearProperty("spring.content.rest.fully-qualified-links");
                    System.clearProperty("spring.content.rest.shortcut-request-mappings.disabled");
                    System.clearProperty("spring.content.rest.shortcut-request-mappings.excludes");
					System.clearProperty("spring.content.rest.overwrite-existing-content");
				});
				It("should have a filesystem properties bean with the correct properties set", () -> {
					AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
					context.register(TestConfig.class);
					context.setServletContext(new MockServletContext());
					context.refresh();

					assertThat(context.getBean(ContentRestAutoConfiguration.ContentRestProperties.class).getBaseUri(), is(URI.create("/contentApi")));
					assertThat(context.getBean(ContentRestAutoConfiguration.ContentRestProperties.class).fullyQualifiedLinks(), is(false));
                    assertThat(context.getBean(ContentRestAutoConfiguration.ContentRestProperties.class).shortcutRequestMappings().disabled(), is(true));
                    assertThat(context.getBean(ContentRestAutoConfiguration.ContentRestProperties.class).shortcutRequestMappings().excludes(), is("GET=a/b,c/d:PUT=*/*"));
					assertThat(context.getBean(ContentRestAutoConfiguration.ContentRestProperties.class).getOverwriteExistingContent(), is(false));

					assertThat(context.getBean(SpringBootContentRestConfigurer.class), is(not(nullValue())));

					context.close();
				});
			});
		});
	}

	@SpringBootApplication
	@ImportAutoConfiguration({ HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class, RepositoryRestMvcAutoConfiguration.class,
			JacksonAutoConfiguration.class, ContentRestAutoConfiguration.class})
	public static class TestConfig {
	}

	@Configuration
	@ImportAutoConfiguration({HypermediaAutoConfiguration.class})
	public static class HypermediaConfig {}

	@Document
	public class TestEntity {
		@Id
		private String id;
		@ContentId
		private String contentId;
	}

	public interface TestEntityRepository extends MongoRepository<TestEntity, String> {
	}

	public interface TestEntityContentRepository extends ContentStore<TestEntity, String> {
	}
}
