package org.springframework.content.fs.boot;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.content.commons.annotations.Content;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.JpaRepository;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.content.fs.boot.autoconfigure.FilesystemContentAutoConfiguration;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class ContentFilesystemAutoConfigurationTest {

	{
		Describe("FilesystemContentAutoConfiguration", () -> {
			Context("given a default configuration", () -> {
				It("should load the context", () -> {
					AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
					context.register(TestConfig.class);
					context.refresh();

					assertThat(context.getBean(TestEntityContentRepository.class), is(not(nullValue())));

					context.close();
				});
			});

			Context("given an environment specifying a filesystem root using spring prefix", () -> {
				/*
				 * Value come from test.properties !!!!! BeforeEach(() -> {
				 * System.setProperty("spring.content.fs.filesystem-root",
				 * "${java.io.tmpdir}/UPPERCASE/NOTATION/"); }); AfterEach(() -> {
				 * System.clearProperty("spring.content.fs.filesystem-root"); });
				 */
				It("should have a filesystem properties bean with the correct root set", () -> {
					AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
					context.register(TestConfig.class);
					context.refresh();

					assertThat(
							context.getBean(FilesystemContentAutoConfiguration.FilesystemProperties.class)
									.getFilesystemRoot(),
							endsWith(File.separator + "UPPERCASE" + File.separator + "NOTATION"));

					context.close();
				});
			});

			Context("given a configuration that contributes a loader bean", () -> {
				It("should have that loader bean in the context", () -> {
					AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
					context.register(ConfigWithLoaderBean.class);
					context.refresh();

					// file system resource loader don't normalize path separator !!!!
					FileSystemResourceLoader loader = context.getBean(FileSystemResourceLoader.class);
					assertThat(loader.getFilesystemRoot(), is("/some/random/path/"));

					context.close();
				});
			});

		});
	}

	@PropertySource("classpath:/test.properties")
	@Configuration
	@ComponentScan
	@EnableAutoConfiguration(exclude = { HibernateJpaAutoConfiguration.class, JdbcTemplateAutoConfiguration.class,
			JpaRepositoriesAutoConfiguration.class, MongoDataAutoConfiguration.class, MongoAutoConfiguration.class })
	public static class TestConfig {
	}

	@Configuration
	@ComponentScan
	@EnableAutoConfiguration(exclude = { HibernateJpaAutoConfiguration.class, JdbcTemplateAutoConfiguration.class,
			JpaRepositoriesAutoConfiguration.class, MongoDataAutoConfiguration.class, MongoAutoConfiguration.class })
	public static class ConfigWithLoaderBean {

		@Bean
		FileSystemResourceLoader fileSystemResourceLoader() {
			return new FileSystemResourceLoader("/some/random/path/");
		}

	}

	@Entity(name = "TestEntity")
	@Content
	public class TestEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private long id;
		@ContentId
		private String contentId;
	}

	public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
	}

	public interface TestEntityContentRepository extends ContentStore<TestEntity, String> {
	}
}
