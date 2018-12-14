package org.springframework.content.renditions.boot;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.renditions.renderers.WordToJpegRenderer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.support.RegistrationPolicy;
import org.springframework.support.TestEntity;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class ContentRenditionsAutoConfigurationTest {

	{
		Describe("ContentRenditionsAutoConfiguration", () -> {
			Context("given a default configuration", () -> {
				It("should load the context and have a WordToJpeg rendered", () -> {
					AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
					context.register(TestConfig.class);
					context.refresh();

					assertThat(context.getBean(WordToJpegRenderer.class),
							is(not(nullValue())));

					context.close();
				});
			});
		});
	}

	@Configuration
	@AutoConfigurationPackage
	@EnableAutoConfiguration
	@EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
	public static class TestConfig {
	}


	public interface TestEntityContentStore
			extends ContentStore<TestEntity, String>, Renderable<TestEntity> {
	}
}
