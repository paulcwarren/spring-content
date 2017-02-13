package internal.org.springframework.content.fs.config;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.content.commons.annotations.Content;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.operations.ContentOperations;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.fs.config.EnableFilesystemContentRepositories;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.content.fs.config.FilesystemProperties;

@RunWith(Ginkgo4jRunner.class)
public class ContentRepositoryTest {

	private AnnotationConfigApplicationContext context;
	{
		Describe("EnableFilesystemContentRepositories", () -> {
			Context("given a context and a configuartion with a filesystem content repository bean", () -> {
				BeforeEach(() -> {
					context = new AnnotationConfigApplicationContext();
					context.register(TestConfig.class);
					context.refresh();
				});
				AfterEach(() -> {
					context.close();
				});
				It("should have a ContentRepository bean", () -> {
					assertThat(context.getBean(TestEntityContentRepository.class), is(not(nullValue())));
				});
				/*
				It("should have a resourceTemplate bean", () -> {
					assertThat(context.getBean("fileResourceTemplate"), is(not(nullValue())));
				});
				It("should have a ContentOperations bean", () -> {
					assertThat(context.getBean(ContentOperations.class), is(not(nullValue())));
				});
				*/
				It("should have a FilesystemProperties bean", () -> {
					assertThat(context.getBean(FilesystemProperties.class), is(not(nullValue())));
					assertThat(context.getBean(FilesystemProperties.class).getFilesystemRoot(), is("/a/b/c"));
				});
			});
			Context("given a context with an empty configuration", () -> {
				BeforeEach(() -> {
					context = new AnnotationConfigApplicationContext();
					context.register(EmptyConfig.class);
					context.refresh();
				});
				AfterEach(() -> {
					context.close();
				});
				It("should not contains any filesystem repository beans", () -> {
					try {
						context.getBean(TestEntityContentRepository.class);
						fail("expected no such bean");
					} catch (NoSuchBeanDefinitionException e) {
						assertThat(true, is(true));
					}
				});
			});
		});
	}


	@Test
	public void noop() {
	}

	@Configuration
	@EnableFilesystemContentRepositories(basePackages="contains.no.fs.repositores")
	@Import(InfrastructureConfig.class)
	public static class EmptyConfig {
	}

	@Configuration
	@EnableFilesystemContentRepositories
	@Import(InfrastructureConfig.class)
	@PropertySource("classpath:/test.properties")
	public static class TestConfig {
	}

	@Configuration
	public static class InfrastructureConfig {
	}

	@Content
	public class TestEntity {
		@ContentId
		private String contentId;
	}

	public interface TestEntityContentRepository extends ContentStore<TestEntity, String> {
	}
}
