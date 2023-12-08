package org.springframework.content.renditions.boot;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrExtensionAutoConfiguration;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.renditions.renderers.JpegToPngRenditionProvider;
import org.springframework.content.renditions.renderers.PdfToJpegRenderer;
import org.springframework.content.renditions.renderers.TextplainToJpegRenderer;
import org.springframework.content.renditions.renderers.WordToJpegRenderer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.support.RegistrationPolicy;
import org.springframework.support.TestEntity;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

//import internal.org.springframework.content.docx4j.WordToHtmlRenditionProvider;
//import internal.org.springframework.content.docx4j.WordToPdfRenditionProvider;
//import internal.org.springframework.content.docx4j.WordToTextRenditionProvider;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class ContentRenditionsAutoConfigurationTest {

	{
		Describe("ContentRenditionsAutoConfiguration", () -> {

			Context("given a default configuration", () -> {

				It("should load the all renderers", () -> {

					AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
					context.register(TestConfig.class);
					context.refresh();

					assertThat(context.getBean(PdfToJpegRenderer.class),is(not(nullValue())));
					assertThat(context.getBean(TextplainToJpegRenderer.class),is(not(nullValue())));
					assertThat(context.getBean(WordToJpegRenderer.class),is(not(nullValue())));

					assertThat(context.getBean(JpegToPngRenditionProvider.class),is(not(nullValue())));
//					assertThat(context.getBean(WordToHtmlRenditionProvider.class),is(not(nullValue())));
//					assertThat(context.getBean(WordToPdfRenditionProvider.class),is(not(nullValue())));
//					assertThat(context.getBean(WordToTextRenditionProvider.class),is(not(nullValue())));

					context.close();
				});
			});
		});
	}

	@Configuration
	@AutoConfigurationPackage
	@EnableAutoConfiguration(exclude={SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
	@EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
	public static class TestConfig {
	}


	public interface TestEntityContentStore
			extends ContentStore<TestEntity, String>, Renderable<TestEntity> {
	}
}
