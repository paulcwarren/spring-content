package org.springframework.content.renditions.boot;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.fs.boot.autoconfigure.FilesystemContentAutoConfiguration;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.content.commons.annotations.Content;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.renditions.renderers.WordToJpegRenderer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads=1)
public class ContentRenditionsAutoConfigurationTest {

	{
		Describe("ContentRenditionsAutoConfiguration", () -> {
			Context("given a default configuration", () -> {
				It("should load the context and have a WordToJpeg rendered", () -> {
					AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
					context.register(TestConfig.class);
					context.refresh();

					assertThat(context.getBean(WordToJpegRenderer.class), is(not(nullValue())));

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

	@Entity
	@Content
	public class TestEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private long id;
		@ContentId
		private String contentId;
	}

	public interface TestEntityContentStore extends ContentStore<TestEntity, String>, Renderable<TestEntity> {
	}
}
